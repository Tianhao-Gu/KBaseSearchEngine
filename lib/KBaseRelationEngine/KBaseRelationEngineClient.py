# -*- coding: utf-8 -*-
############################################################
#
# Autogenerated by the KBase type compiler -
# any changes made here will be overwritten
#
############################################################

from __future__ import print_function
# the following is a hack to get the baseclient to import whether we're in a
# package or not. This makes pep8 unhappy hence the annotations.
try:
    # baseclient and this client are in a package
    from .baseclient import BaseClient as _BaseClient  # @UnusedImport
except:
    # no they aren't
    from baseclient import BaseClient as _BaseClient  # @Reimport


class KBaseRelationEngine(object):

    def __init__(
            self, url=None, timeout=30 * 60, user_id=None,
            password=None, token=None, ignore_authrc=False,
            trust_all_ssl_certificates=False,
            auth_svc='https://kbase.us/services/authorization/Sessions/Login'):
        if url is None:
            raise ValueError('A url is required')
        self._service_ver = None
        self._client = _BaseClient(
            url, timeout=timeout, user_id=user_id, password=password,
            token=token, ignore_authrc=ignore_authrc,
            trust_all_ssl_certificates=trust_all_ssl_certificates,
            auth_svc=auth_svc)

    def search_types(self, params, context=None):
        """
        Search for number of objects of each type matching constrains.
        :param params: instance of type "SearchTypesInput" (Input parameters
           for search_types method.) -> structure: parameter "match_filter"
           of type "MatchFilter" (Optional rules of defining constrains for
           object properties including values of keywords or metadata/system
           properties (like object name, creation time range) or full-text
           search in all properties.) -> structure: parameter
           "full_text_in_all" of String, parameter "access_group_id" of Long,
           parameter "object_name" of String, parameter "parent_guid" of type
           "GUID" (Global user identificator. It has structure like this:
           <data-source-code>:<full-reference>[:<sub-type>/<sub-id>]),
           parameter "timestamp" of type "MatchValue" (Optional rules of
           defining constraints for values of particular term (keyword).
           Appropriate field depends on type of keyword. For instance in case
           of integer type 'int_value' should be used. In case of range
           constraint rather than single value 'min_*' and 'max_*' fields
           should be used. You may omit one of ends of range to achieve '<='
           or '>=' comparison. Ends are always included for range
           constrains.) -> structure: parameter "value" of String, parameter
           "int_value" of Long, parameter "double_value" of Double, parameter
           "bool_value" of type "boolean" (A boolean. 0 = false, other =
           true.), parameter "min_int" of Long, parameter "max_int" of Long,
           parameter "min_date" of Long, parameter "max_date" of Long,
           parameter "min_double" of Double, parameter "max_double" of
           Double, parameter "lookupInKeys" of mapping from String to type
           "MatchValue" (Optional rules of defining constraints for values of
           particular term (keyword). Appropriate field depends on type of
           keyword. For instance in case of integer type 'int_value' should
           be used. In case of range constraint rather than single value
           'min_*' and 'max_*' fields should be used. You may omit one of
           ends of range to achieve '<=' or '>=' comparison. Ends are always
           included for range constrains.) -> structure: parameter "value" of
           String, parameter "int_value" of Long, parameter "double_value" of
           Double, parameter "bool_value" of type "boolean" (A boolean. 0 =
           false, other = true.), parameter "min_int" of Long, parameter
           "max_int" of Long, parameter "min_date" of Long, parameter
           "max_date" of Long, parameter "min_double" of Double, parameter
           "max_double" of Double, parameter "access_filter" of type
           "AccessFilter" (Optional rules of access constrains. -
           with_private - include data found in workspaces not marked as
           public, default value is true, - with_public - include data found
           in public workspaces, default value is false, - with_all_history -
           include all versions (last one and all old versions) of objects
           matching constrains, default value is false.) -> structure:
           parameter "with_private" of type "boolean" (A boolean. 0 = false,
           other = true.), parameter "with_public" of type "boolean" (A
           boolean. 0 = false, other = true.), parameter "with_all_history"
           of type "boolean" (A boolean. 0 = false, other = true.)
        :returns: instance of type "SearchTypesOutput" (Output results of
           search_types method.) -> structure: parameter "type_to_count" of
           mapping from String to Long, parameter "search_time" of Long
        """
        return self._client.call_method(
            'KBaseRelationEngine.search_types',
            [params], self._service_ver, context)

    def search_objects(self, params, context=None):
        """
        Search for objects of particular type matching constrains.
        :param params: instance of type "SearchObjectsInput" (Input
           parameters for 'search_objects' method.) -> structure: parameter
           "object_type" of String, parameter "match_filter" of type
           "MatchFilter" (Optional rules of defining constrains for object
           properties including values of keywords or metadata/system
           properties (like object name, creation time range) or full-text
           search in all properties.) -> structure: parameter
           "full_text_in_all" of String, parameter "access_group_id" of Long,
           parameter "object_name" of String, parameter "parent_guid" of type
           "GUID" (Global user identificator. It has structure like this:
           <data-source-code>:<full-reference>[:<sub-type>/<sub-id>]),
           parameter "timestamp" of type "MatchValue" (Optional rules of
           defining constraints for values of particular term (keyword).
           Appropriate field depends on type of keyword. For instance in case
           of integer type 'int_value' should be used. In case of range
           constraint rather than single value 'min_*' and 'max_*' fields
           should be used. You may omit one of ends of range to achieve '<='
           or '>=' comparison. Ends are always included for range
           constrains.) -> structure: parameter "value" of String, parameter
           "int_value" of Long, parameter "double_value" of Double, parameter
           "bool_value" of type "boolean" (A boolean. 0 = false, other =
           true.), parameter "min_int" of Long, parameter "max_int" of Long,
           parameter "min_date" of Long, parameter "max_date" of Long,
           parameter "min_double" of Double, parameter "max_double" of
           Double, parameter "lookupInKeys" of mapping from String to type
           "MatchValue" (Optional rules of defining constraints for values of
           particular term (keyword). Appropriate field depends on type of
           keyword. For instance in case of integer type 'int_value' should
           be used. In case of range constraint rather than single value
           'min_*' and 'max_*' fields should be used. You may omit one of
           ends of range to achieve '<=' or '>=' comparison. Ends are always
           included for range constrains.) -> structure: parameter "value" of
           String, parameter "int_value" of Long, parameter "double_value" of
           Double, parameter "bool_value" of type "boolean" (A boolean. 0 =
           false, other = true.), parameter "min_int" of Long, parameter
           "max_int" of Long, parameter "min_date" of Long, parameter
           "max_date" of Long, parameter "min_double" of Double, parameter
           "max_double" of Double, parameter "sorting_rules" of list of type
           "SortingRule" (Rule for sorting found results. 'key_name',
           'is_timestamp' and 'is_object_name' are alternative way of
           defining what property if used for sorting. Default order is
           ascending (if 'descending' field is not set).) -> structure:
           parameter "is_timestamp" of type "boolean" (A boolean. 0 = false,
           other = true.), parameter "is_object_name" of type "boolean" (A
           boolean. 0 = false, other = true.), parameter "key_name" of
           String, parameter "descending" of type "boolean" (A boolean. 0 =
           false, other = true.), parameter "access_filter" of type
           "AccessFilter" (Optional rules of access constrains. -
           with_private - include data found in workspaces not marked as
           public, default value is true, - with_public - include data found
           in public workspaces, default value is false, - with_all_history -
           include all versions (last one and all old versions) of objects
           matching constrains, default value is false.) -> structure:
           parameter "with_private" of type "boolean" (A boolean. 0 = false,
           other = true.), parameter "with_public" of type "boolean" (A
           boolean. 0 = false, other = true.), parameter "with_all_history"
           of type "boolean" (A boolean. 0 = false, other = true.), parameter
           "pagination" of type "Pagination" (Pagination rules. Default
           values are: start = 0, count = 50.) -> structure: parameter
           "start" of Long, parameter "count" of Long, parameter
           "post_processing" of type "PostProcessing" (Rules for what to
           return about found objects. skip_info - do not include brief info
           for object ('guid, 'parent_guid', 'object_name' and 'timestamp'
           fields in ObjectData structure), skip_keys - do not include
           keyword values for object ('key_props' field in ObjectData
           structure), skip_data - do not include raw data for object ('data'
           and 'parent_data' fields in ObjectData structure), ids_only -
           shortcut to mark all three skips as true.) -> structure: parameter
           "ids_only" of type "boolean" (A boolean. 0 = false, other =
           true.), parameter "skip_info" of type "boolean" (A boolean. 0 =
           false, other = true.), parameter "skip_keys" of type "boolean" (A
           boolean. 0 = false, other = true.), parameter "skip_data" of type
           "boolean" (A boolean. 0 = false, other = true.), parameter
           "data_includes" of list of String
        :returns: instance of type "SearchObjectsOutput" (Output results for
           'search_objects' method. 'pagination' and 'sorting_rules' fields
           show actual input for pagination and sorting. total - total number
           of found objects. search_time - common time in milliseconds
           spent.) -> structure: parameter "pagination" of type "Pagination"
           (Pagination rules. Default values are: start = 0, count = 50.) ->
           structure: parameter "start" of Long, parameter "count" of Long,
           parameter "sorting_rules" of list of type "SortingRule" (Rule for
           sorting found results. 'key_name', 'is_timestamp' and
           'is_object_name' are alternative way of defining what property if
           used for sorting. Default order is ascending (if 'descending'
           field is not set).) -> structure: parameter "is_timestamp" of type
           "boolean" (A boolean. 0 = false, other = true.), parameter
           "is_object_name" of type "boolean" (A boolean. 0 = false, other =
           true.), parameter "key_name" of String, parameter "descending" of
           type "boolean" (A boolean. 0 = false, other = true.), parameter
           "objects" of list of type "ObjectData" (Properties of found object
           including metadata, raw data and keywords.) -> structure:
           parameter "guid" of type "GUID" (Global user identificator. It has
           structure like this:
           <data-source-code>:<full-reference>[:<sub-type>/<sub-id>]),
           parameter "parent_guid" of type "GUID" (Global user identificator.
           It has structure like this:
           <data-source-code>:<full-reference>[:<sub-type>/<sub-id>]),
           parameter "object_name" of String, parameter "timestamp" of Long,
           parameter "parent_data" of unspecified object, parameter "data" of
           unspecified object, parameter "key_props" of mapping from String
           to String, parameter "total" of Long, parameter "search_time" of
           Long
        """
        return self._client.call_method(
            'KBaseRelationEngine.search_objects',
            [params], self._service_ver, context)

    def get_objects(self, params, context=None):
        """
        Retrieve objects by their GUIDs.
        :param params: instance of type "GetObjectsInput" (Input parameters
           for get_objects method.) -> structure: parameter "guids" of list
           of type "GUID" (Global user identificator. It has structure like
           this: <data-source-code>:<full-reference>[:<sub-type>/<sub-id>]),
           parameter "post_processing" of type "PostProcessing" (Rules for
           what to return about found objects. skip_info - do not include
           brief info for object ('guid, 'parent_guid', 'object_name' and
           'timestamp' fields in ObjectData structure), skip_keys - do not
           include keyword values for object ('key_props' field in ObjectData
           structure), skip_data - do not include raw data for object ('data'
           and 'parent_data' fields in ObjectData structure), ids_only -
           shortcut to mark all three skips as true.) -> structure: parameter
           "ids_only" of type "boolean" (A boolean. 0 = false, other =
           true.), parameter "skip_info" of type "boolean" (A boolean. 0 =
           false, other = true.), parameter "skip_keys" of type "boolean" (A
           boolean. 0 = false, other = true.), parameter "skip_data" of type
           "boolean" (A boolean. 0 = false, other = true.), parameter
           "data_includes" of list of String
        :returns: instance of type "GetObjectsOutput" (Output results of
           get_objects method.) -> structure: parameter "objects" of list of
           type "ObjectData" (Properties of found object including metadata,
           raw data and keywords.) -> structure: parameter "guid" of type
           "GUID" (Global user identificator. It has structure like this:
           <data-source-code>:<full-reference>[:<sub-type>/<sub-id>]),
           parameter "parent_guid" of type "GUID" (Global user identificator.
           It has structure like this:
           <data-source-code>:<full-reference>[:<sub-type>/<sub-id>]),
           parameter "object_name" of String, parameter "timestamp" of Long,
           parameter "parent_data" of unspecified object, parameter "data" of
           unspecified object, parameter "key_props" of mapping from String
           to String, parameter "search_time" of Long
        """
        return self._client.call_method(
            'KBaseRelationEngine.get_objects',
            [params], self._service_ver, context)

    def list_types(self, params, context=None):
        """
        List registered searchable object types.
        :param params: instance of type "ListTypesInput" (Input parameters
           for list_types method. type_name - optional parameter; if not
           specified all types are described.) -> structure: parameter
           "type_name" of String
        :returns: instance of type "ListTypesOutput" (Output results of
           list_types method.) -> structure: parameter "types" of mapping
           from String to type "TypeDescriptor" (Description of searchable
           object type including details about keywords. TODO: add more
           details like parent type, relations, primary key, ...) ->
           structure: parameter "type_name" of String, parameter
           "type_ui_title" of String, parameter "keys" of list of type
           "KeyDescription" (Description of searchable type keyword. -
           key_value_type can be one of {'string', 'integer', 'double',
           'boolean'}, - hidden - if true then this keyword provides values
           for other keywords (like in 'link_key') and is not supposed to be
           shown. - link_key - optional field pointing to another keyword
           (which is often hidden) providing GUID to build external URL to.)
           -> structure: parameter "key_name" of String, parameter
           "key_ui_title" of String, parameter "key_value_type" of String,
           parameter "hidden" of type "boolean" (A boolean. 0 = false, other
           = true.), parameter "link_key" of String
        """
        return self._client.call_method(
            'KBaseRelationEngine.list_types',
            [params], self._service_ver, context)

    def status(self, context=None):
        return self._client.call_method('KBaseRelationEngine.status',
                                        [], self._service_ver, context)
