FROM docker.dbc.dk/payara5-micro:os
MAINTAINER osteam@dbc.dk

ENV APPLICATION_NAME="opensearch6" \
    CLUSTER_NAME="devel"

LABEL  \
      BADGERFISH_RULES_LOCATION="Uri of badgerfish rules YAML file (optional - mostly for debugging)" \
      CACHE_EXPIRE_x="Rule for cache, syntax 'policy:(number)unit:(max)'. Policy is one of: CREATED, MODIFIED, TOUCHED, ACCESSED or ETERNAL. Unis is one of: 'ms', 's', 'm' or 'h'. max is max count of cache entries" \
      CLUSTER_NAME="Name of the stage the application is deployed into (required)" \
      CONTEXT_ROOT="context-root of application without leading slash (default: /)" \
      DEFAULT_REPOSITORY="The default repository to search in (required)" \
      X_FORWARDED_FOR="Comma seperated list of host, host-range or network, where trusted proxies are located (for acquiring remote host ip)"

ADD config.d/* config.d/
ADD *.war wars/ROOT.war
