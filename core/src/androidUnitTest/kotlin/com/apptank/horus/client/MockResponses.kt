package com.apptank.horus.client

const val MOCK_RESPONSE_GET_MIGRATION = """
    [
  {
    "entity": "measures_values",
    "type": "editable",
    "attributes": [
      {
        "name": "id",
        "version": 1,
        "type": "primary_key_string",
        "nullable": false
      },
      {
        "name": "sync_owner_id",
        "version": 1,
        "type": "string",
        "nullable": false
      },
      {
        "name": "sync_hash",
        "version": 1,
        "type": "string",
        "nullable": false
      },
      {
        "name": "sync_created_at",
        "version": 1,
        "type": "timestamp",
        "nullable": false
      },
      {
        "name": "sync_updated_at",
        "version": 1,
        "type": "timestamp",
        "nullable": false
      },
      {
        "name": "measure",
        "version": 1,
        "type": "enum",
        "nullable": false
      },
      {
        "name": "unit",
        "version": 1,
        "type": "enum",
        "nullable": false
      },
      {
        "name": "value",
        "version": 1,
        "type": "float",
        "nullable": false
      }
    ],
    "current_version": 1
  },
  {
    "entity": "farms",
    "type": "editable",
    "attributes": [
      {
        "name": "id",
        "version": 1,
        "type": "primary_key_string",
        "nullable": false
      },
      {
        "name": "sync_owner_id",
        "version": 1,
        "type": "string",
        "nullable": false
      },
      {
        "name": "sync_hash",
        "version": 1,
        "type": "string",
        "nullable": false
      },
      {
        "name": "sync_created_at",
        "version": 1,
        "type": "timestamp",
        "nullable": false
      },
      {
        "name": "sync_updated_at",
        "version": 1,
        "type": "timestamp",
        "nullable": false
      },
      {
        "name": "name",
        "version": 1,
        "type": "string",
        "nullable": false
      },
      {
        "name": "destination",
        "version": 1,
        "type": "enum",
        "nullable": false
      },
      {
        "name": "mv_area_total",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "mv_area_cow_farming",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "measure_milk",
        "version": 1,
        "type": "enum",
        "nullable": false
      },
      {
        "name": "measure_weight",
        "version": 1,
        "type": "enum",
        "nullable": false
      },
      {
        "name": "type",
        "version": 1,
        "type": "enum",
        "nullable": false
      },
      {
        "name": "relations_one_of_many",
        "version": 1,
        "type": "relation_one_of_many",
        "nullable": false,
        "related": [
          {
            "entity": "farms_metadata",
            "type": "editable",
            "attributes": [
              {
                "name": "id",
                "version": 1,
                "type": "primary_key_string",
                "nullable": false
              },
              {
                "name": "sync_owner_id",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_hash",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_created_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "sync_updated_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "farm_id",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "name",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "value",
                "version": 1,
                "type": "string",
                "nullable": false
              }
            ],
            "current_version": 1
          },
          {
            "entity": "lots",
            "type": "editable",
            "attributes": [
              {
                "name": "id",
                "version": 1,
                "type": "primary_key_string",
                "nullable": false
              },
              {
                "name": "sync_owner_id",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_hash",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_created_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "sync_updated_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "farm_id",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "name",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "relations_one_of_many",
                "version": 1,
                "type": "relation_one_of_many",
                "nullable": false,
                "related": [
                  {
                    "entity": "animals_lots",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "lot_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "int",
                        "nullable": false
                      }
                    ],
                    "current_version": 1
                  }
                ]
              }
            ],
            "current_version": 1
          },
          {
            "entity": "animals",
            "type": "editable",
            "attributes": [
              {
                "name": "id",
                "version": 1,
                "type": "primary_key_string",
                "nullable": false
              },
              {
                "name": "sync_owner_id",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_hash",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_created_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "sync_updated_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "farm_id",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "name",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "code",
                "version": 1,
                "type": "string",
                "nullable": true
              },
              {
                "name": "gender",
                "version": 1,
                "type": "enum",
                "nullable": false
              },
              {
                "name": "type",
                "version": 1,
                "type": "enum",
                "nullable": false
              },
              {
                "name": "purpose",
                "version": 1,
                "type": "enum",
                "nullable": false
              },
              {
                "name": "branding_iron_id",
                "version": 1,
                "type": "uuid",
                "nullable": true
              },
              {
                "name": "sale_status",
                "version": 1,
                "type": "enum",
                "nullable": false
              },
              {
                "name": "stage",
                "version": 1,
                "type": "enum",
                "nullable": false
              },
              {
                "name": "reproductive_status",
                "version": 1,
                "type": "enum",
                "nullable": false
              },
              {
                "name": "health_status",
                "version": 1,
                "type": "enum",
                "nullable": false
              },
              {
                "name": "inside",
                "version": 1,
                "type": "boolean",
                "nullable": false
              },
              {
                "name": "relations_one_of_many",
                "version": 1,
                "type": "relation_one_of_many",
                "nullable": false,
                "related": [
                  {
                    "entity": "animal_deaths",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "cause",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "notes",
                        "version": 1,
                        "type": "text",
                        "nullable": true
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_facts",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "fact",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "reference_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": true
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_favorites",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_images",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "type",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "path",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_weights",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "weight_type",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "mv_weight",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "diet_type",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "in_treatment",
                        "version": 1,
                        "type": "boolean",
                        "nullable": false
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_inseminations",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "type",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "notes",
                        "version": 1,
                        "type": "text",
                        "nullable": true
                      },
                      {
                        "name": "relations_one_of_many",
                        "version": 1,
                        "type": "relation_one_of_many",
                        "nullable": false,
                        "related": [
                          {
                            "entity": "animal_inseminations_metadata",
                            "type": "editable",
                            "attributes": [
                              {
                                "name": "id",
                                "version": 1,
                                "type": "primary_key_string",
                                "nullable": false
                              },
                              {
                                "name": "sync_owner_id",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              },
                              {
                                "name": "sync_hash",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              },
                              {
                                "name": "sync_created_at",
                                "version": 1,
                                "type": "timestamp",
                                "nullable": false
                              },
                              {
                                "name": "sync_updated_at",
                                "version": 1,
                                "type": "timestamp",
                                "nullable": false
                              },
                              {
                                "name": "animal_insemination_id",
                                "version": 1,
                                "type": "uuid",
                                "nullable": false
                              },
                              {
                                "name": "name",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              },
                              {
                                "name": "value",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              }
                            ],
                            "current_version": 1
                          }
                        ]
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_treatments",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date_starts",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "date_ends",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "days",
                        "version": 1,
                        "type": "int",
                        "nullable": false
                      },
                      {
                        "name": "name",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "diagnostic",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "medicine",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "notes",
                        "version": 1,
                        "type": "string",
                        "nullable": true
                      },
                      {
                        "name": "type",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "sick",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_alerts",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "type",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "date_starts",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "date_ends",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "description",
                        "version": 1,
                        "type": "string",
                        "nullable": true
                      },
                      {
                        "name": "relations_one_of_many",
                        "version": 1,
                        "type": "relation_one_of_many",
                        "nullable": false,
                        "related": [
                          {
                            "entity": "animal_alerts_metadata",
                            "type": "editable",
                            "attributes": [
                              {
                                "name": "id",
                                "version": 1,
                                "type": "primary_key_string",
                                "nullable": false
                              },
                              {
                                "name": "sync_owner_id",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              },
                              {
                                "name": "sync_hash",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              },
                              {
                                "name": "sync_created_at",
                                "version": 1,
                                "type": "timestamp",
                                "nullable": false
                              },
                              {
                                "name": "sync_updated_at",
                                "version": 1,
                                "type": "timestamp",
                                "nullable": false
                              },
                              {
                                "name": "animal_alert_id",
                                "version": 1,
                                "type": "uuid",
                                "nullable": false
                              },
                              {
                                "name": "name",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              },
                              {
                                "name": "value",
                                "version": 1,
                                "type": "string",
                                "nullable": false
                              }
                            ],
                            "current_version": 1
                          }
                        ]
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_pregnant_checks",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "is_pregnant",
                        "version": 1,
                        "type": "boolean",
                        "nullable": false
                      },
                      {
                        "name": "notes",
                        "version": 1,
                        "type": "text",
                        "nullable": true
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_abortions",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "gender",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "notes",
                        "version": 1,
                        "type": "text",
                        "nullable": true
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_mastitis",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "severity",
                        "version": 1,
                        "type": "json",
                        "nullable": false
                      },
                      {
                        "name": "notes",
                        "version": 1,
                        "type": "text",
                        "nullable": true
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_milking",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "lot_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "date",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "stage",
                        "version": 1,
                        "type": "enum",
                        "nullable": false
                      },
                      {
                        "name": "mv_milk",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "mv_concentrates",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      }
                    ],
                    "current_version": 1
                  }
                ]
              },
              {
                "name": "relations_one_of_one",
                "version": 1,
                "type": "relation_one_of_one",
                "nullable": false,
                "related": [
                  {
                    "entity": "animal_tree",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "father_animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": true
                      },
                      {
                        "name": "mother_animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": true
                      },
                      {
                        "name": "surmother_animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": true
                      }
                    ],
                    "current_version": 1
                  },
                  {
                    "entity": "animal_breeds_crosses",
                    "type": "editable",
                    "attributes": [
                      {
                        "name": "id",
                        "version": 1,
                        "type": "primary_key_string",
                        "nullable": false
                      },
                      {
                        "name": "sync_owner_id",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_hash",
                        "version": 1,
                        "type": "string",
                        "nullable": false
                      },
                      {
                        "name": "sync_created_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "sync_updated_at",
                        "version": 1,
                        "type": "timestamp",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "base_animal_breed_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "base_animal_breed_percentage",
                        "version": 1,
                        "type": "int",
                        "nullable": false
                      },
                      {
                        "name": "cross_animal_breed_id",
                        "version": 1,
                        "type": "uuid",
                        "nullable": true
                      },
                      {
                        "name": "cross_animal_breed_percentage",
                        "version": 1,
                        "type": "int",
                        "nullable": true
                      }
                    ],
                    "current_version": 1
                  }
                ]
              }
            ],
            "current_version": 1
          },
          {
            "entity": "branding_irons",
            "type": "editable",
            "attributes": [
              {
                "name": "id",
                "version": 1,
                "type": "primary_key_string",
                "nullable": false
              },
              {
                "name": "sync_owner_id",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_hash",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_created_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "sync_updated_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "farm_id",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "name",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "image",
                "version": 1,
                "type": "string",
                "nullable": true
              }
            ],
            "current_version": 1
          },
          {
            "entity": "farm_milk_sales",
            "type": "editable",
            "attributes": [
              {
                "name": "id",
                "version": 1,
                "type": "primary_key_string",
                "nullable": false
              },
              {
                "name": "sync_owner_id",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_hash",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_created_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "sync_updated_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "farm_id",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "date",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "paddock",
                "version": 1,
                "type": "int",
                "nullable": true
              },
              {
                "name": "mv_internal_consume",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "mv_animal_consume",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "mv_milk_total",
                "version": 1,
                "type": "uuid",
                "nullable": false
              }
            ],
            "current_version": 1
          }
        ]
      },
      {
        "name": "relations_one_of_one",
        "version": 1,
        "type": "relation_one_of_one",
        "nullable": false,
        "related": [
          {
            "entity": "farm_locations",
            "type": "editable",
            "attributes": [
              {
                "name": "id",
                "version": 1,
                "type": "primary_key_string",
                "nullable": false
              },
              {
                "name": "sync_owner_id",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_hash",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_created_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "sync_updated_at",
                "version": 1,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "farm_id",
                "version": 1,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "country",
                "version": 1,
                "type": "string",
                "nullable": false
              },
              {
                "name": "city_id",
                "version": 1,
                "type": "int",
                "nullable": false
              },
              {
                "name": "longitude",
                "version": 1,
                "type": "float",
                "nullable": false
              },
              {
                "name": "latitude",
                "version": 1,
                "type": "float",
                "nullable": false
              },
              {
                "name": "address",
                "version": 1,
                "type": "string",
                "nullable": false
              }
            ],
            "current_version": 1
          }
        ]
      }
    ],
    "current_version": 1
  },
  {
    "entity": "animal_breeds",
    "type": "lookup",
    "attributes": [
      {
        "name": "id",
        "version": 1,
        "type": "primary_key_integer",
        "nullable": false
      },
      {
        "name": "type",
        "version": 1,
        "type": "enum",
        "nullable": false
      },
      {
        "name": "name",
        "version": 1,
        "type": "string",
        "nullable": false
      }
    ],
    "current_version": 1
  }
]
"""