FROM docker.dbc.dk/payara5-micro:os
MAINTAINER osteam@dbc.dk

ENV APPLICATION_NAME="opensearch6" \
    CLUSTER_NAME="devel" \
    CACHE_EXPIRE_profile=CREATED:5h:1000 \
    CACHE_EXPIRE_profile_error=CREATED:1m:1000 \
    CACHE_EXPIRE_resultset=ACCESSED:5m:10000 \
    CACHE_EXPIRE_records=ACCESSED:2m:10000

LABEL  \
      BADGERFISH_RULES_LOCATION="Uri of badgerfish rules YAML file (optional - mostly for debugging)" \
      CACHE_EXPIRE_x="Rule for cache, syntax 'policy:(number)unit:(max)'. Policy is one of: CREATED, MODIFIED, TOUCHED, ACCESSED or ETERNAL. Unis is one of: 'ms', 's', 'm' or 'h'. max is max count of cache entries" \
      CLUSTER_NAME="Name of the stage the application is deployed into (required)" \
      CONTEXT_ROOT="context-root of application without leading slash (default: /)" \
      COREPO_SOLR_URL="address of corpeo-solr (required)" \
      COREPO_CONTENT_SERVICE_URL="address of corpeo-content-service (required)" \
      DEFAULT_REPOSITORY="The default repository to search in (required)" \
      OPEN_AGENCY_URL="address of open-agency webservice (required)" \
      X_FORWARDED_FOR="Comma seperated list of host, host-range or network, where trusted proxies are located (for acquiring remote host ip)"

ADD config.d/* config.d/
ADD *.war wars/ROOT.war
