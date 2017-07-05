package kbaserelationengine.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kbaserelationengine.common.ObjectJsonPath;
import kbaserelationengine.parse.ObjectParseException;
import us.kbase.common.service.UObject;

public class ObjectTypeParsingRules {
    private String globalObjectType;
    private String uiTypeName;
    private String storageType;
    private String storageObjectType;
    private String innerSubType;
    private ObjectJsonPath pathToSubObjects;
    private List<IndexingRules> indexingRules;
    private ObjectJsonPath primaryKeyPath;
    private List<RelationRules> relationRules;
    
    public String getGlobalObjectType() {
        return globalObjectType;
    }
    
    public void setGlobalObjectType(String globalObjectType) {
        this.globalObjectType = globalObjectType;
    }
    
    public String getUiTypeName() {
        return uiTypeName;
    }
    
    public void setUiTypeName(String uiTypeName) {
        this.uiTypeName = uiTypeName;
    }
    
    public String getStorageType() {
        return storageType;
    }
    
    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
    
    public String getStorageObjectType() {
        return storageObjectType;
    }
    
    public void setStorageObjectType(String storageObjectType) {
        this.storageObjectType = storageObjectType;
    }
    
    public String getInnerSubType() {
        return innerSubType;
    }
    
    public void setInnerSubType(String innerSubType) {
        this.innerSubType = innerSubType;
    }
    
    public ObjectJsonPath getPathToSubObjects() {
        return pathToSubObjects;
    }
    
    public void setPathToSubObjects(ObjectJsonPath pathToSubObjects) {
        this.pathToSubObjects = pathToSubObjects;
    }

    public List<IndexingRules> getIndexingRules() {
        return indexingRules;
    }
    
    public void setIndexingRules(List<IndexingRules> indexingRules) {
        this.indexingRules = indexingRules;
    }
    
    public ObjectJsonPath getPrimaryKeyPath() {
        return primaryKeyPath;
    }
    
    public void setPrimaryKeyPath(ObjectJsonPath primaryKeyPath) {
        this.primaryKeyPath = primaryKeyPath;
    }
    
    public List<RelationRules> getRelationRules() {
        return relationRules;
    }
    
    public void setRelationRules(
            List<RelationRules> foreignKeyLookupRules) {
        this.relationRules = foreignKeyLookupRules;
    }

    public static ObjectTypeParsingRules fromFile(File file) 
            throws ObjectParseException, IOException {
        try (InputStream is = new FileInputStream(file)) {
            return fromStream(is);
        }
    }

    public static ObjectTypeParsingRules fromJson(String json) throws ObjectParseException {
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = UObject.transformStringToObject(json, Map.class);
        return fromObject(obj);
    }

    public static ObjectTypeParsingRules fromStream(InputStream is) 
            throws IOException, ObjectParseException {
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = UObject.getMapper().readValue(is, Map.class);
        return fromObject(obj);
    }

    public static ObjectTypeParsingRules fromObject(Map<String, Object> obj) 
            throws ObjectParseException {
        ObjectTypeParsingRules ret = new ObjectTypeParsingRules();
        ret.setGlobalObjectType((String)obj.get("global-object-type"));
        ret.setUiTypeName((String)obj.get("ui-type-name"));
        ret.setStorageType((String)obj.get("storage-type"));
        ret.setStorageObjectType((String)obj.get("storage-object-type"));
        ret.setInnerSubType((String)obj.get("inner-sub-type"));
        ret.setPathToSubObjects(getPath((String)obj.get("path-to-sub-objects")));
        // Indexing
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> indexingRules = 
                (List<Map<String, Object>>)obj.get("indexing-rules");
        if (indexingRules != null) {
            ret.setIndexingRules(new ArrayList<>());
            for (Map<String, Object> rulesObj : indexingRules) {
                IndexingRules rules = new IndexingRules();
                String path = (String)rulesObj.get("path");
                if (path != null) {
                    rules.setPath(new ObjectJsonPath(path));
                }
                Boolean fullText = (Boolean)rulesObj.get("full-text");
                if (fullText != null) {
                    rules.setFullText(fullText);
                }
                rules.setKeywordType((String)rulesObj.get("keyword-type"));
                rules.setKeyName((String)rulesObj.get("key-name"));
                rules.setTransform((String)rulesObj.get("transform"));
                Boolean fromParent = (Boolean)rulesObj.get("from-parent");
                if (fromParent != null) {
                    rules.setFromParent(fromParent);
                }
                Boolean derivedKey = (Boolean)rulesObj.get("derived-key");
                if (derivedKey != null) {
                    rules.setDerivedKey(derivedKey);
                }
                Boolean notIndexed = (Boolean)rulesObj.get("not-indexed");
                if (notIndexed != null) {
                    rules.setNotIndexed(notIndexed);
                }
                rules.setSourceKey((String)rulesObj.get("source-key"));
                rules.setTargetObjectType((String)rulesObj.get("source-key"));
                rules.setSubobjectIdKey((String)rulesObj.get("subobject-id-key"));
                rules.setConstantValue(rulesObj.get("constant-value"));
                rules.setOptionalDefaultValue(rulesObj.get("optional-default-value"));
                rules.setTargetObjectType((String)rulesObj.get("target-object-type"));
                rules.setUiName((String)rulesObj.get("ui-name"));
                Boolean uiHidden = (Boolean)rulesObj.get("ui-hidden");
                if (uiHidden != null) {
                    rules.setUiHidden(uiHidden);
                }
                rules.setUiLinkKey((String)rulesObj.get("ui-link-key"));
                ret.getIndexingRules().add(rules);
            }
        }
        ret.setPrimaryKeyPath(getPath((String)obj.get("primary-key-path")));
        // Relations
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relationRules = 
                (List<Map<String, Object>>)obj.get("relation-rules");
        if (relationRules != null) {
            ret.setRelationRules(new ArrayList<>());
            for (Map<String, Object> rulesObj : relationRules) {
                RelationRules rules = new RelationRules();
                String pathText = (String)rulesObj.get("path");
                if (pathText != null) {
                    rules.setPath(new ObjectJsonPath(pathText));
                }
                rules.setTargetObjectType((String)rulesObj.get("target-object-type"));
                rules.setRelationType((String)rulesObj.get("relation-type"));
                ret.getRelationRules().add(rules);
            }
        }
        return ret;
    }
    
    private static ObjectJsonPath getPath(String path) throws ObjectParseException {
        return path == null ? null : new ObjectJsonPath(path);
    }
}
