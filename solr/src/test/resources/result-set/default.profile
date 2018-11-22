{
  "openSearchProfileResponse": {
    "profile": [
      {
        "profileName": {
          "$": "test",
          "@": "oa"
        },
        "source": [
          {
            "sourceName": {
              "$": "Artikelbasens Anmeldelser",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "870971-anmeld",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isReviewOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasReview",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "Artikelbasens avisartikler",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "870971-avis",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:continues",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:continuedIn",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:discusses",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:discussedIn",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCreatorDescription",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "Artikelbasens avisartikler med infomedialink",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceContainedIn": {
              "$": "870971-avis",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "870971-avisinf",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:continues",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:continuedIn",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:discusses",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:discussedIn",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "Artikelbasens tidsskriftsartikler",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "870971-tsart",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:continues",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:continuedIn",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOpenUrl",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:discusses",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:discussedIn",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCreatorDescription",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "Bibliotekets katalog",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "default-katalog",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isAnalysisOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasAnalysis",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isSoundtrackOfGame",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasSoundtrack",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isReviewOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasReview",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasAdaptation",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:isAdaptationOf",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isManuscriptOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasManuscript",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOpenUrl",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcbib:isPartOfManifestation",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCreatorDescription",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isSoundtrackOfMovie",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasSoundtrack",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "eReolen (hvis også lydbøger - vælg kilden Netlydbog)",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceContainedIn": {
              "$": "870970-basis",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "150015-ereol",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isReviewOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasReview",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "eReolen børnematerialer",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceContainedIn": {
              "$": "870970-basis",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "150015-ereolchld",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isReviewOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasReview",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOpenUrl",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "eReolen Global (e-bøger fra OverDrive)",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "150061-ebog",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOpenUrl",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCover",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "eReolen Global (netlydbøger fra OverDrive)",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "150061-netlydbog",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCover",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "eReolen Global ung (e-bøger fra OverDrive)",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "150061-ebogung",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOpenUrl",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCover",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "eReolen Global ung (netlydboger fra OverDrive)",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "150061-netlydung",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCover",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          },
          {
            "sourceName": {
              "$": "Folkebiblioteker og Nationalbibliografi (alle 870970-poster, til FBS)",
              "@": "oa"
            },
            "sourceSearchable": {
              "$": "1",
              "@": "oa"
            },
            "sourceIdentifier": {
              "$": "870970-basis",
              "@": "oa"
            },
            "relation": [
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOnlineAccess",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isAnalysisOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasAnalysis",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isSoundtrackOfGame",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasSoundtrack",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isReviewOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasReview",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasAdaptation",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:isAdaptationOf",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isManuscriptOf",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasManuscript",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasOpenUrl",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcbib:isPartOfManifestation",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:hasCreatorDescription",
                  "@": "oa"
                },
                "@": "oa"
              },
              {
                "rdfLabel": {
                  "$": "dbcaddi:isSoundtrackOfMovie",
                  "@": "oa"
                },
                "rdfInverse": {
                  "$": "dbcaddi:hasSoundtrack",
                  "@": "oa"
                },
                "@": "oa"
              }
            ],
            "@": "oa"
          }
        ],
        "@": "oa"
      }
    ],
    "@": "oa"
  },
  "@namespaces": {
    "oa": "http://oss.dbc.dk/ns/openagency"
  }
}
