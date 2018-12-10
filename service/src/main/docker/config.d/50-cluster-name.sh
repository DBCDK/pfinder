if [ x"$CLUSTER_NAME" = x ]; then
	echo "CLUSTER_NAME is unset" >&2
	exit 1
fi

ADD_PAYARA_ARGS="${ADD_PAYARA_ARGS:+$ADD_PAYARA_ARGS }--clustername ${CLUSTER_NAME} --instancegroup OpenSearch"
