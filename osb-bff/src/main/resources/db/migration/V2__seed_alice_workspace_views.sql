-- Dev seed: Alice workspace views (from live bff_ui_config).

INSERT INTO bff_ui_config (user_name, config_key, payload)
VALUES (
    'alice',
    'workspace-views',
    $json$
{
  "views": [
    {
      "id": "standard",
      "name": "Standard",
      "layout": {
        "grid": {
          "root": {
            "data": [
              {
                "data": {
                  "id": "2",
                  "views": [
                    "catalogs"
                  ],
                  "activeView": "catalogs"
                },
                "size": 473,
                "type": "leaf"
              },
              {
                "data": {
                  "id": "3",
                  "views": [
                    "catalog",
                    "plans",
                    "offerings",
                    "workflows",
                    "templates",
                    "plan",
                    "offering"
                  ],
                  "activeView": "catalog"
                },
                "size": 1397,
                "type": "leaf"
              }
            ],
            "size": 868,
            "type": "branch"
          },
          "width": 1870,
          "height": 868,
          "orientation": "HORIZONTAL"
        },
        "panels": {
          "plan": {
            "id": "plan",
            "title": "Plan",
            "contentComponent": "PlanPanel"
          },
          "plans": {
            "id": "plans",
            "title": "Pläne",
            "contentComponent": "PlansPanel"
          },
          "catalog": {
            "id": "catalog",
            "title": "Katalog",
            "contentComponent": "CatalogPanel"
          },
          "catalogs": {
            "id": "catalogs",
            "title": "Kataloge",
            "contentComponent": "CatalogsPanel"
          },
          "offering": {
            "id": "offering",
            "title": "Offering",
            "contentComponent": "OfferingPanel"
          },
          "offerings": {
            "id": "offerings",
            "title": "Offerings",
            "contentComponent": "OfferingsPanel"
          },
          "templates": {
            "id": "templates",
            "title": "Templates",
            "contentComponent": "TemplatesPanel"
          },
          "workflows": {
            "id": "workflows",
            "title": "Workflows",
            "contentComponent": "WorkflowsPanel"
          }
        },
        "activeGroup": "3"
      }
    },
    {
      "id": "9206c9cf-d5eb-4817-8190-043f62a9bfc2",
      "name": "Instances",
      "layout": {
        "grid": {
          "root": {
            "data": [
              {
                "data": {
                  "id": "2",
                  "views": [
                    "instances"
                  ],
                  "activeView": "instances"
                },
                "size": 935,
                "type": "leaf"
              },
              {
                "data": {
                  "id": "4",
                  "views": [
                    "instance"
                  ],
                  "activeView": "instance"
                },
                "size": 935,
                "type": "leaf"
              }
            ],
            "size": 868,
            "type": "branch"
          },
          "width": 1870,
          "height": 868,
          "orientation": "HORIZONTAL"
        },
        "panels": {
          "instance": {
            "id": "instance",
            "title": "Instanz",
            "contentComponent": "InstancePanel"
          },
          "instances": {
            "id": "instances",
            "title": "Instanzen",
            "contentComponent": "InstancesPanel"
          }
        },
        "activeGroup": "4"
      }
    },
    {
      "id": "b1f9c414-853f-409b-9fdc-811b10cb68c2",
      "name": "Templates",
      "layout": {
        "grid": {
          "root": {
            "data": [
              {
                "data": {
                  "id": "1",
                  "views": [
                    "templates",
                    "template"
                  ],
                  "activeView": "templates"
                },
                "size": 1870,
                "type": "leaf"
              }
            ],
            "size": 868,
            "type": "branch"
          },
          "width": 1870,
          "height": 868,
          "orientation": "HORIZONTAL"
        },
        "panels": {
          "template": {
            "id": "template",
            "title": "Template",
            "contentComponent": "TemplatePanel"
          },
          "templates": {
            "id": "templates",
            "title": "Templates",
            "contentComponent": "TemplatesPanel"
          }
        },
        "activeGroup": "1"
      }
    },
    {
      "id": "f3889378-5b77-4966-9ca0-003d68a38f88",
      "name": "Workflows",
      "layout": {
        "grid": {
          "root": {
            "data": [
              {
                "data": {
                  "id": "2",
                  "views": [
                    "workflows"
                  ],
                  "activeView": "workflows"
                },
                "size": 603,
                "type": "leaf"
              },
              {
                "data": {
                  "id": "3",
                  "views": [
                    "workflow"
                  ],
                  "activeView": "workflow"
                },
                "size": 1267,
                "type": "leaf"
              }
            ],
            "size": 868,
            "type": "branch"
          },
          "width": 1870,
          "height": 868,
          "orientation": "HORIZONTAL"
        },
        "panels": {
          "workflow": {
            "id": "workflow",
            "title": "Workflow",
            "contentComponent": "WorkflowPanel"
          },
          "workflows": {
            "id": "workflows",
            "title": "Workflows",
            "contentComponent": "WorkflowsPanel"
          }
        },
        "activeGroup": "3"
      }
    },
    {
      "id": "a7c3e1f2-4b59-4d8e-9c1a-2e6f8b0d5a41",
      "name": "Clients",
      "layout": {
        "grid": {
          "root": {
            "data": [
              {
                "data": {
                  "id": "2",
                  "views": [
                    "httpClients",
                    "kubernetesClients",
                    "gitClients"
                  ],
                  "activeView": "httpClients"
                },
                "size": 935,
                "type": "leaf"
              },
              {
                "data": {
                  "id": "4",
                  "views": [
                    "httpClient",
                    "kubernetesClient",
                    "gitClient"
                  ],
                  "activeView": "httpClient"
                },
                "size": 935,
                "type": "leaf"
              }
            ],
            "size": 868,
            "type": "branch"
          },
          "width": 1870,
          "height": 868,
          "orientation": "HORIZONTAL"
        },
        "panels": {
          "httpClients": {
            "id": "httpClients",
            "title": "HTTP-Clients",
            "contentComponent": "HttpClientsPanel"
          },
          "httpClient": {
            "id": "httpClient",
            "title": "HTTP-Client",
            "contentComponent": "HttpClientPanel"
          },
          "kubernetesClients": {
            "id": "kubernetesClients",
            "title": "Kubernetes-Clients",
            "contentComponent": "KubernetesClientsPanel"
          },
          "kubernetesClient": {
            "id": "kubernetesClient",
            "title": "Kubernetes-Client",
            "contentComponent": "KubernetesClientPanel"
          },
          "gitClients": {
            "id": "gitClients",
            "title": "Git-Clients",
            "contentComponent": "GitClientsPanel"
          },
          "gitClient": {
            "id": "gitClient",
            "title": "Git-Client",
            "contentComponent": "GitClientPanel"
          }
        },
        "activeGroup": "4"
      }
    }
  ],
  "activeViewId": "9206c9cf-d5eb-4817-8190-043f62a9bfc2"
}
$json$::jsonb
)
ON CONFLICT (user_name, config_key) DO NOTHING;
