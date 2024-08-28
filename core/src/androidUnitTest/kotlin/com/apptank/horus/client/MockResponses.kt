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
        "nullable": false,
        "options": [
          "w",
          "v",
          "a"
        ]
      },
      {
        "name": "unit",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz",
          "mg",
          "t",
          "m3",
          "L",
          "mL",
          "cm3",
          "dm3",
          "in3",
          "ft3",
          "yd3",
          "m2",
          "cm2",
          "mm2",
          "km2",
          "ha",
          "in2",
          "ft2",
          "yd2",
          "ac"
        ]
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
        "nullable": false,
        "options": [
          "1",
          "2",
          "3",
          "4",
          "5"
        ]
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
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz",
          "mg",
          "t",
          "m3",
          "L",
          "mL",
          "cm3",
          "dm3",
          "in3",
          "ft3",
          "yd3",
          "m2",
          "cm2",
          "mm2",
          "km2",
          "ha",
          "in2",
          "ft2",
          "yd2",
          "ac"
        ]
      },
      {
        "name": "measure_weight",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz",
          "mg",
          "t",
          "m3",
          "L",
          "mL",
          "cm3",
          "dm3",
          "in3",
          "ft3",
          "yd3",
          "m2",
          "cm2",
          "mm2",
          "km2",
          "ha",
          "in2",
          "ft2",
          "yd2",
          "ac"
        ]
      },
      {
        "name": "type",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "1",
          "2"
        ]
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
        "nullable": false,
        "options": [
          "1",
          "2",
          "3",
          "4",
          "5"
        ]
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

// 7 Entities
const val DATA_MIGRATION_VERSION_1 = """
    [
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
        "version": 2,
        "type": "string",
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
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz"
        ]
      },
      {
        "name": "measure_weight",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz",
          "mg"
        ]
      },
      {
        "name": "type",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "1",
          "2"
        ]
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
              }
            ],
            "current_version": 1
          }
        ]
      }
    ],
    "current_version": 1
  }
]
"""
const val DATA_MIGRATION_VERSION_2 = """
    [
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
        "version": 2,
        "type": "string",
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
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz"
        ]
      },
      {
        "name": "measure_weight",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz",
          "mg"
        ]
      },
      {
        "name": "type",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "1",
          "2"
        ]
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
                "version": 2,
                "type": "string",
                "nullable": false
              }
            ],
            "current_version": 2
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
                "version": 2,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "name",
                "version": 2,
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
                        "version": 2,
                        "type": "uuid",
                        "nullable": false
                      }
                    ],
                    "current_version": 2
                  }
                ]
              }
            ],
            "current_version": 2
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
                "version": 2,
                "type": "float",
                "nullable": false
              }
            ],
            "current_version": 2
          }
        ]
      }
    ],
    "current_version": 2
  }
]
"""
const val DATA_MIGRATION_VERSION_3 = """
    [
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
        "version": 2,
        "type": "string",
        "nullable": false
      },
      {
        "name": "destination",
        "version": 3,
        "type": "enum",
        "nullable": false,
        "options": [
          "1",
          "2",
          "3",
          "4",
          "5"
        ]
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
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz"
        ]
      },
      {
        "name": "measure_weight",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "kg",
          "g",
          "lb",
          "oz",
          "mg"
        ]
      },
      {
        "name": "type",
        "version": 1,
        "type": "enum",
        "nullable": false,
        "options": [
          "1",
          "2"
        ]
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
                "version": 2,
                "type": "string",
                "nullable": false
              },
              {
                "name": "value",
                "version": 3,
                "type": "string",
                "nullable": false
              }
            ],
            "current_version": 3
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
                "version": 2,
                "type": "uuid",
                "nullable": false
              },
              {
                "name": "name",
                "version": 2,
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
                        "version": 2,
                        "type": "uuid",
                        "nullable": false
                      },
                      {
                        "name": "animal_id",
                        "version": 3,
                        "type": "int",
                        "nullable": false
                      }
                    ],
                    "current_version": 3
                  }
                ]
              }
            ],
            "current_version": 2
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
                "version": 2,
                "type": "float",
                "nullable": false
              },
              {
                "name": "latitude",
                "version": 3,
                "type": "float",
                "nullable": false
              },
              {
                "name": "address",
                "version": 3,
                "type": "string",
                "nullable": false
              }
            ],
            "current_version": 3
          }
        ]
      }
    ],
    "current_version": 3
  }
]
"""