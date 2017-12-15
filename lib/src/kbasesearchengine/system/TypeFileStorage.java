package kbasesearchengine.system;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import kbasesearchengine.main.LineLogger;
import kbasesearchengine.parse.ObjectParseException;

public class TypeFileStorage implements TypeStorage {
    
    //TODO JAVADOC
    //TODO TEST
    
    private static final String TYPE_STORAGE = "[TypeStorage]";
    // as opposed to file types for mappings
    private static final Set<String> ALLOWED_FILE_TYPES_FOR_TYPES =
            new HashSet<>(Arrays.asList(".json", ".yaml"));
    
    private final Map<String, ArrayList<ObjectTypeParsingRules>> searchTypes = new HashMap<>();
    private final Map<CodeAndType, TypeMapping> storageTypes;
    
    private Map<CodeAndType, TypeMapping> processTypesDir(
            final Path typesDir,
            final LineLogger logger)
            throws IOException, ObjectParseException, TypeParseException {
        final Map<String, Path> typeToFile = new HashMap<>();
        final Map<CodeAndType, TypeMapping.Builder> storageTypes = new HashMap<>(); 
        // this is gross, but works. https://stackoverflow.com/a/20130475/643675
        for (Path file : (Iterable<Path>) Files.list(typesDir)::iterator) {
            if (Files.isRegularFile(file) && isAllowedFileType(file)) {
                final ObjectTypeParsingRules type = ObjectTypeParsingRulesUtils
                        .fromFile(file.toFile());
                final SearchObjectType searchType = type.getGlobalObjectType();
                if (typeToFile.containsKey(searchType)) {
                    throw new TypeParseException(String.format(
                            "Multiple definitions for the same search type %s in files %s and %s",
                            searchType, file, typeToFile.get(searchType)));
                }
                typeToFile.put(searchType.getType(), file);
                addType(type);
                final CodeAndType cnt = new CodeAndType(type);
                if (!storageTypes.containsKey(cnt)) {
                    storageTypes.put(cnt, TypeMapping.getBuilder(cnt.storageCode, cnt.storageType)
                            .withNullableDefaultSearchType(searchType.getType()) //TODO VERS take version into account
                            .withNullableSourceInfo(file.toString()));
                } else {
                    storageTypes.get(cnt).withNullableDefaultSearchType(searchType.getType()); //TODO VERS take version into account
                }
                logger.logInfo(String.format("%s Processed type tranformation file with storage " +
                        "code %s, storage type %s and search type %s: %s",
                        TYPE_STORAGE, cnt.storageCode, cnt.storageType, searchType.getType(),
                        file));
            } else {
                logger.logInfo(TYPE_STORAGE + " Skipping file in type tranformation directory: " +
                        file);
            }
        }
        verifyNoMissingVersions();
        final Map<CodeAndType, TypeMapping> ret = new HashMap<>();
        storageTypes.keySet().stream().forEach(k -> ret.put(k, storageTypes.get(k).build()));
        return ret;
    }

    private void verifyNoMissingVersions() throws TypeParseException {
        for (final String type: searchTypes.keySet()) {
            final ArrayList<ObjectTypeParsingRules> vers = searchTypes.get(type);
            for (int i = 0; i < vers.size(); i++) {
                if (vers.get(i) == null) {
                    throw new TypeParseException(
                            String.format("Missing version %s of type %s", i + 1, type));
                }
            }
        }
    }

    private void addType(final ObjectTypeParsingRules rules) {
        final String type = rules.getGlobalObjectType().getType();
        final int version = rules.getGlobalObjectType().getVersion();
        if (!searchTypes.containsKey(type)) {
            searchTypes.put(type, new ArrayList<>(version));
        }
        // make the list big enough for the version
        // might want to have a check at some point that there are no nulls when all the files
        // are processed
        final ArrayList<ObjectTypeParsingRules> versions = searchTypes.get(type);
        for (int i = versions.size(); i < version; i++) {
            versions.add(null);
        }
        versions.set(version - 1, rules);
    }

    private boolean isAllowedFileType(final Path file) {
        final String path = file.toString();
        for (final String allowedExtension: ALLOWED_FILE_TYPES_FOR_TYPES) {
            if (path.endsWith(allowedExtension)) {
                return true;
            }
        }
        return false;
    }
    
    private static class CodeAndType {
        private final String storageCode;
        private final String storageType;
        
        private CodeAndType(final String storageCode, final String storageType) {
            this.storageCode = storageCode;
            this.storageType = storageType;
        }
        
        private CodeAndType(final ObjectTypeParsingRules type) {
            this.storageCode = type.getStorageObjectType().getStorageCode();
            this.storageType = type.getStorageObjectType().getType();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("CodeAndType [storageCode=");
            builder.append(storageCode);
            builder.append(", storageType=");
            builder.append(storageType);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((storageCode == null) ? 0 : storageCode.hashCode());
            result = prime * result
                    + ((storageType == null) ? 0 : storageType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CodeAndType other = (CodeAndType) obj;
            if (storageCode == null) {
                if (other.storageCode != null) {
                    return false;
                }
            } else if (!storageCode.equals(other.storageCode)) {
                return false;
            }
            if (storageType == null) {
                if (other.storageType != null) {
                    return false;
                }
            } else if (!storageType.equals(other.storageType)) {
                return false;
            }
            return true;
        }
    }
    
    public TypeFileStorage(
            final Path typesDir,
            final Path mappingsDir,
            final Map<String, TypeMappingParser> parsers,
            final LineLogger logger)
            throws IOException, ObjectParseException, TypeParseException {
        storageTypes = processTypesDir(typesDir, logger);
        final Map<CodeAndType, TypeMapping> mappings = processMappingsDir(
                mappingsDir, parsers, logger);
        for (final CodeAndType cnt: mappings.keySet()) {
            if (storageTypes.containsKey(cnt)) {
                logger.logInfo(String.format(
                        "%s Overriding type mapping for storage code %s and storage type %s " +
                        "from type transformation file with definition from type mapping file %s",
                        TYPE_STORAGE, cnt.storageCode, cnt.storageType,
                        mappings.get(cnt).getSourceInfo().get()));
            }
            storageTypes.put(cnt, mappings.get(cnt));
        }
    }
    
    private Map<CodeAndType, TypeMapping> processMappingsDir(
            final Path mappingsDir,
            final Map<String, TypeMappingParser> parsers,
            final LineLogger logger)
            throws IOException, TypeParseException {
        final Map<CodeAndType, TypeMapping> ret = new HashMap<>();
        // this is gross, but works. https://stackoverflow.com/a/20130475/643675
        for (Path file : (Iterable<Path>) Files.list(mappingsDir)::iterator) {
            if (Files.isRegularFile(file)) {
                final String ext = FilenameUtils.getExtension(file.toString());
                final TypeMappingParser parser = parsers.get(ext);
                if (parser != null) {
                    final Set<TypeMapping> mappings;
                    try (final InputStream is = Files.newInputStream(file)) {
                        mappings = parser.parse(new BufferedInputStream(is), file.toString());
                    }
                    for (final TypeMapping map: mappings) {
                        final CodeAndType cnt = new CodeAndType(
                                map.getStorageCode(), map.getStorageType());
                        if (ret.containsKey(cnt)) {
                            throw new TypeParseException(String.format(
                                    "Type collision for type %s in storage %s. " +
                                    "Type is specified in both files %s and %s.",
                                    cnt.storageType, cnt.storageCode,
                                    ret.get(cnt).getSourceInfo().get(),
                                    map.getSourceInfo().get()));
                        }
                        //TODO VERS this needs to be version aware
                        for (final String searchType: map.getSearchTypes()) {
                            if (!searchTypes.containsKey(searchType)) {
                                throw new TypeParseException(String.format(
                                        "The search type %s specified in source code/type %s/%s " +
                                        "does not have an equivalent tranform type. File: %s",
                                        searchType, cnt.storageCode, cnt.storageType,
                                        map.getSourceInfo().get()));
                            }
                        }
                        ret.put(cnt, map);
                    }
                } else {
                    logger.logInfo(TYPE_STORAGE + " Skipping file in type mapping directory: " +
                            file);
                }
            } else {
                logger.logInfo(TYPE_STORAGE + "Skipping entry in type mapping directory: " + file);
            }
        }
        return ret;
    }

    @Override
    public List<ObjectTypeParsingRules> listObjectTypes() {
        //TODO VERS is returning the last version ok? At least document in interface. Provide option?
        return searchTypes.values().stream().map(l -> l.get(l.size() - 1))
                .collect(Collectors.toList());
    }
    
    @Override
    public ObjectTypeParsingRules getObjectType(final SearchObjectType type)
            throws NoSuchTypeException {
        //TODO CODE seems like throwing an error here for the guid transform case is a late fail. The check should occur when the OTPRs are being built.
        if (searchTypes.containsKey(type.getType())) {
            final ArrayList<ObjectTypeParsingRules> vers = searchTypes.get(type.getType());
            if (type.getVersion() > vers.size()) {
                throw new NoSuchTypeException(String.format("No type %s_%s found",
                        type.getType(), type.getVersion()));
            }
            return vers.get(type.getVersion() - 1);
        } else {
            throw new NoSuchTypeException(String.format("No type %s_%s found",
                    type.getType(), type.getVersion()));
        }
    }
    
    @Override
    public List<ObjectTypeParsingRules> listObjectTypesByStorageObjectType(
            final StorageObjectType storageObjectType) {
        final TypeMapping mapping = storageTypes.get(
                new CodeAndType(storageObjectType.getStorageCode(), storageObjectType.getType()));
        if (mapping == null) {
            return Collections.emptyList();
        }
        final Set<String> types = mapping.getSearchTypes(storageObjectType.getVersion());
        final List<ObjectTypeParsingRules> ret = new LinkedList<>();
        for (final String t: types) {
            ret.add(searchTypes.get(t).get(0));  //TODO VERS needs to be version aware
        }
        return ret;
    }
}
