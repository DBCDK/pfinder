
case "$CONTEXT_ROOT" in
    "")
        echo "using context root: /"
        ;;
    *[^-0-9a-zA-Z._]*)
        echo "Contextroot with unsupported characters" >&2
        exit 1
        ;;
    *)
        echo "using context root: /$CONTEXT_ROOT"
        mv wars/ROOT.war wars/"$CONTEXT_ROOT".war
        ;;
esac
