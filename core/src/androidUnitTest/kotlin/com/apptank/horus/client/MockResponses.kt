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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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

const val MOCK_RESPONSE_GET_DATA = """
    [
    {
        "entity": "measures_values",
        "data": {
            "id": "3093a07a-543b-336b-9ca8-4c3bf207aeb5",
            "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
            "sync_hash": "89424a4c2efa23f3b6a0caa03bfdcf66dd3d6d31c0559c8c286f82fcd3798f6d",
            "sync_created_at": 1725044885,
            "sync_updated_at": 1725044885,
            "measure": "w",
            "unit": "kg",
            "value": 10
        }
    },
    {
        "entity": "farms",
        "data": {
            "id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
            "sync_hash": "e3d47ef233c70cc1819b8cf63818d7f79c3dc8d54d90c8241b76eae67be7254d",
            "sync_created_at": 1725051913,
            "sync_updated_at": 1725051913,
            "name": "Finca Los Alpinos",
            "destination": "1",
            "mv_area_total": "5701514f-fd5c-4417-b3d9-6fdcd8ee746b",
            "mv_area_cow_farming": "4635a0a7-8548-4327-b74d-9ca88a6ccf90",
            "measure_milk": "L",
            "measure_weight": "kg",
            "type": "1",
            "_metadata": [],
            "_lots": [],
            "_animals": [
                {
                    "entity": "animals",
                    "data": {
                        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
                        "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
                        "sync_hash": "d2b4f095f8634c7c19ec7a173fa3ce84830e0a20f20d75a8749cc7a5f6cfbb83",
                        "sync_created_at": 1725051913,
                        "sync_updated_at": 1725051913,
                        "farm_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
                        "name": "Camila",
                        "code": "9012345",
                        "gender": "f",
                        "type": "1",
                        "purpose": "2",
                        "sale_status": "0",
                        "stage": "4",
                        "reproductive_status": "1",
                        "health_status": "1",
                        "inside": 1,
                        "_deaths": [],
                        "_facts": [],
                        "_favorites": [],
                        "_images": [],
                        "_weights": [],
                        "_inseminations": [],
                        "_treatments": [],
                        "_alerts": [],
                        "_pregnantChecks": [],
                        "_abortions": [],
                        "_breed": [],
                        "_mastitis": [],
                        "_milking": []
                    }
                }
            ],
            "_irons": [],
            "_milkSales": []
        }
    }
]
"""

const val MOCK_RESPONSE_GET_DATA_ENTITY = """
    [
    {
        "entity": "farms",
        "data": {
            "id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
            "sync_hash": "e3d47ef233c70cc1819b8cf63818d7f79c3dc8d54d90c8241b76eae67be7254d",
            "sync_created_at": 1725051913,
            "sync_updated_at": 1725051913,
            "name": "Finca Los Alpinos",
            "destination": "1",
            "mv_area_total": "5701514f-fd5c-4417-b3d9-6fdcd8ee746b",
            "mv_area_cow_farming": "4635a0a7-8548-4327-b74d-9ca88a6ccf90",
            "measure_milk": "L",
            "measure_weight": "kg",
            "type": "1",
            "_metadata": [],
            "_lots": [],
            "_animals": [
                {
                    "entity": "animals",
                    "data": {
                        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
                        "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
                        "sync_hash": "d2b4f095f8634c7c19ec7a173fa3ce84830e0a20f20d75a8749cc7a5f6cfbb83",
                        "sync_created_at": 1725051913,
                        "sync_updated_at": 1725051913,
                        "farm_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
                        "name": "Camila",
                        "code": "9012345",
                        "gender": "f",
                        "type": "1",
                        "purpose": "2",
                        "sale_status": "0",
                        "stage": "4",
                        "reproductive_status": "1",
                        "health_status": "1",
                        "inside": 1,
                        "_deaths": [],
                        "_facts": [],
                        "_favorites": [],
                        "_images": [],
                        "_weights": [],
                        "_inseminations": [],
                        "_treatments": [],
                        "_alerts": [],
                        "_pregnantChecks": [],
                        "_abortions": [],
                        "_breed": [],
                        "_mastitis": [],
                        "_milking": []
                    }
                }
            ],
            "_irons": [],
            "_milkSales": []
        }
    }
]
"""

const val MOCK_RESPONSE_GET_QUEUE_ACTIONS = """
    [
    {
        "action": "INSERT",
        "entity": "measures_values",
        "data": {
            "id": "3093a07a-543b-336b-9ca8-4c3bf207aeb5",
            "unit": "kg",
            "value": 10,
            "measure": "w"
        },
        "actioned_at": 1725037164,
        "synced_at": 1725044885
    },
    {
        "action": "INSERT",
        "entity": "measures_values",
        "data": {
            "id": "3093a07a-543b-336b-9ca8-4c3bf207aeb4",
            "unit": "kg",
            "value": 10,
            "measure": "w"
        },
        "actioned_at": 1725037000,
        "synced_at": 1725048331
    },
    {
        "action": "UPDATE",
        "entity": "measures_values",
        "data": {
            "id": "3093a07a-543b-336b-9ca8-4c3bf207aeb4",
            "attributes": {
                "unit": "kg",
                "value": 20,
                "measure": "w"
            }
        },
        "actioned_at": 1725037064,
        "synced_at": 1725048331
    },
    {
        "action": "DELETE",
        "entity": "measures_values",
        "data": {
            "id": "3093a07a-543b-336b-9ca8-4c3bf207aeb4"
        },
        "actioned_at": 1725037164,
        "synced_at": 1725048331
    },
    {
        "action": "INSERT",
        "entity": "farms",
        "data": {
            "id": "ea5c701b-5439-47b4-adcf-b91802bec259",
            "name": "Finca Los Alpinos",
            "type": 1,
            "destination": 1,
            "measure_milk": "L",
            "mv_area_total": "0e83bf78-0e91-4c9d-88c7-d760d3dd8ef8",
            "measure_weight": "kg",
            "mv_area_cow_farming": "568c3ceb-c323-4b43-8269-c9104bff8431"
        },
        "actioned_at": 1725037000,
        "synced_at": 1725048957
    },
    {
        "action": "INSERT",
        "entity": "farms",
        "data": {
            "id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "name": "Finca Los Alpinos",
            "type": 1,
            "destination": 1,
            "measure_milk": "L",
            "mv_area_total": "5701514f-fd5c-4417-b3d9-6fdcd8ee746b",
            "measure_weight": "kg",
            "mv_area_cow_farming": "4635a0a7-8548-4327-b74d-9ca88a6ccf90"
        },
        "actioned_at": 1725037000,
        "synced_at": 1725051913
    },
    {
        "action": "INSERT",
        "entity": "animals",
        "data": {
            "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
            "code": "9012345",
            "name": "Camila",
            "type": 1,
            "stage": 4,
            "gender": "f",
            "inside": true,
            "farm_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "purpose": 2,
            "sale_status": "0",
            "health_status": 1,
            "branding_iron_id": null,
            "reproductive_status": 1
        },
        "actioned_at": 1725037001,
        "synced_at": 1725051913
    }
]
"""

const val MOCK_RESPONSE_POST_VALIDATE_DATA = """
    [
    {
        "entity": "farms",
        "hash": {
            "expected": "86f90952b15a4c31dd1fcebbda7d807611e304eb45793b91f1d27db2024d210f",
            "obtained": "86f90952b15a4c31dd1fcebbda7d807611e304eb45793b91f1d27db2024d210f",
            "matched": true
        }
    },
    {
        "entity": "animals",
        "hash": {
            "expected": "6f2488fa2911ca67861dcc5d1549874d8bf9c76f37cb5c1bfad5c7b5f52550c8",
            "obtained": "5467ab59e695c4c470c3c3d2912aca5530087b2286878f7e74e725163468f94f",
            "matched": false
        }
    }
]
"""

const val MOCK_RESPONSE_POST_VALIDATE_HASHING = """
    {
    "expected": "4168cbdb0ca6923e633bf61e795c61b0c74908334d71191afb223cad7c91bf64",
    "obtained": "4168cbdb0ca6923e633bf61e795c61b0c74908334d71191afb223cad7c91bf64",
    "matched": true
}
"""

const val MOCK_RESPONSE_INTERNAL_SERVER_ERROR = """
 {
    "message": "Internal server error"
}
"""

const val MOCK_RESPONSE_GET_LAST_QUEUE_ACTION = """
    {
    "action": "INSERT",
    "entity": "animals",
    "data": {
        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
        "code": "9012345",
        "name": "Camila",
        "type": 1,
        "stage": 4,
        "gender": "f",
        "inside": true,
        "farm_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
        "purpose": 2,
        "sale_status": "0",
        "health_status": 1,
        "branding_iron_id": null,
        "reproductive_status": 1
    },
    "actioned_at": 1725037001,
    "synced_at": 1725051913
}
"""

const val MOCK_RESPONSE_GET_ENTITY_HASHES = """
    [
    {
        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
        "sync_hash": "d2b4f095f8634c7c19ec7a173fa3ce84830e0a20f20d75a8749cc7a5f6cfbb83"
    }
]
"""

// 6 Entities
const val DATA_MIGRATION_VERSION_1 ="""
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "version": 3,
                "type": "primary_key_string",
                "nullable": false
              },
              {
                "name": "sync_owner_id",
                "version": 3,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_hash",
                "version": 3,
                "type": "string",
                "nullable": false
              },
              {
                "name": "sync_created_at",
                "version": 3,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "sync_updated_at",
                "version": 3,
                "type": "timestamp",
                "nullable": false
              },
              {
                "name": "farm_id",
                "version": 3,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "farms"
              },
              {
                "name": "name",
                "version": 3,
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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
                "nullable": false,
                "linked_entity": "farms"
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

const val DATA_MIGRATION_WITH_LOOKUP_AND_EDITABLE = """
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