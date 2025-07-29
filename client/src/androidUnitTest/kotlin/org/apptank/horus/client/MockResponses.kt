package org.apptank.horus.client

const val MOCK_RESPONSE_GET_MIGRATION = """
    [
  {
    "entity": "measures",
    "type": "writable",
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
        "name": "custom_uuid",
        "version": 1,
        "type": "custom",
        "nullable": true,
        "regex": "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}${'$'}"
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
    "entity": "products",
    "type": "writable",
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
        "name": "mv_size",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "mv_variant",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "volume",
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
        "name": "weight",
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
            "entity": "products_metadata",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
                    "entity": "categories_lots",
                    "type": "writable",
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
                        "name": "product_id",
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
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
            "entity": "product_milk_sales",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
                "name": "mv_product_consume",
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
            "entity": "product_locations",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
    "entity": "product_breeds",
    "type": "readable",
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
        "entity": "measures",
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
        "entity": "products",
        "data": {
            "id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
            "sync_hash": "e3d47ef233c70cc1819b8cf63818d7f79c3dc8d54d90c8241b76eae67be7254d",
            "sync_created_at": 1725051913,
            "sync_updated_at": 1725051913,
            "name": "Finca Los Alpinos",
            "destination": "1",
            "mv_size": "5701514f-fd5c-4417-b3d9-6fdcd8ee746b",
            "mv_variant": "4635a0a7-8548-4327-b74d-9ca88a6ccf90",
            "volume": "L",
            "weight": "kg",
            "type": "1",
            "_metadata": [],
            "_lots": [],
            "_categories": [
                {
                    "entity": "categories",
                    "data": {
                        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
                        "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
                        "sync_hash": "d2b4f095f8634c7c19ec7a173fa3ce84830e0a20f20d75a8749cc7a5f6cfbb83",
                        "sync_created_at": 1725051913,
                        "sync_updated_at": 1725051913,
                        "product_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
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
                        "_facts": [],
                        "_favorites": [],
                        "_images": [],
                        "_weights": [],
                        "_alerts": [],
                        "_milking": []
                    }
                }
            ]
        }
    }
]
"""

const val MOCK_RESPONSE_GET_DATA_ENTITY = """
    [
    {
        "entity": "products",
        "data": {
            "id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
            "sync_hash": "e3d47ef233c70cc1819b8cf63818d7f79c3dc8d54d90c8241b76eae67be7254d",
            "sync_created_at": 1725051913,
            "sync_updated_at": 1725051913,
            "name": "Finca Los Alpinos",
            "destination": "1",
            "mv_size": "5701514f-fd5c-4417-b3d9-6fdcd8ee746b",
            "mv_variant": "4635a0a7-8548-4327-b74d-9ca88a6ccf90",
            "volume": "L",
            "weight": "kg",
            "type": "1",
            "_metadata": [],
            "_lots": [],
            "_categories": [
                {
                    "entity": "categories",
                    "data": {
                        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
                        "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
                        "sync_hash": "d2b4f095f8634c7c19ec7a173fa3ce84830e0a20f20d75a8749cc7a5f6cfbb83",
                        "sync_created_at": 1725051913,
                        "sync_updated_at": 1725051913,
                        "product_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
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
        "entity": "measures",
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
        "entity": "measures",
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
        "entity": "measures",
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
        "entity": "measures",
        "data": {
            "id": "3093a07a-543b-336b-9ca8-4c3bf207aeb4"
        },
        "actioned_at": 1725037164,
        "synced_at": 1725048331
    },
    {
        "action": "INSERT",
        "entity": "products",
        "data": {
            "id": "ea5c701b-5439-47b4-adcf-b91802bec259",
            "name": "Finca Los Alpinos",
            "type": 1,
            "destination": 1,
            "volume": "L",
            "mv_size": "0e83bf78-0e91-4c9d-88c7-d760d3dd8ef8",
            "weight": "kg",
            "mv_variant": "568c3ceb-c323-4b43-8269-c9104bff8431"
        },
        "actioned_at": 1725037000,
        "synced_at": 1725048957
    },
    {
        "action": "INSERT",
        "entity": "products",
        "data": {
            "id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "name": "Finca Los Alpinos",
            "type": 1,
            "destination": 1,
            "volume": "L",
            "mv_size": "5701514f-fd5c-4417-b3d9-6fdcd8ee746b",
            "weight": "kg",
            "mv_variant": "4635a0a7-8548-4327-b74d-9ca88a6ccf90"
        },
        "actioned_at": 1725037000,
        "synced_at": 1725051913
    },
    {
        "action": "INSERT",
        "entity": "categories",
        "data": {
            "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
            "code": "9012345",
            "name": "Camila",
            "type": 1,
            "stage": 4,
            "gender": "f",
            "inside": true,
            "product_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
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
        "entity": "products",
        "hash": {
            "expected": "86f90952b15a4c31dd1fcebbda7d807611e304eb45793b91f1d27db2024d210f",
            "obtained": "86f90952b15a4c31dd1fcebbda7d807611e304eb45793b91f1d27db2024d210f",
            "matched": true
        }
    },
    {
        "entity": "categories",
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
    "entity": "categories",
    "data": {
        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
        "code": "9012345",
        "name": "Camila",
        "type": 1,
        "stage": 4,
        "gender": "f",
        "inside": true,
        "product_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
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

const val MOCK_RESPONSE_GET_DATA_SHARED = """
    [
  {
    "entity": "readable_fake_entity",
    "data": {
      "id": 1,
      "name": "Douglas Haley",
      "type": "type3"
    }
  },
  {
    "entity": "readable_fake_entity",
    "data": {
      "id": 2,
      "name": "Dessie Hickle",
      "type": "type2"
    }
  },
  {
    "entity": "readable_fake_entity",
    "data": {
      "id": 3,
      "name": "Ralph Buckridge",
      "type": "type2"
    }
  },
  {
    "entity": "readable_fake_entity",
    "data": {
      "id": 4,
      "name": "Mrs. Nora Kihn III",
      "type": "type2"
    }
  },
  {
    "entity": "readable_fake_entity",
    "data": {
      "id": 5,
      "name": "Oceane Thiel",
      "type": "type2"
    }
  }
]
"""

// 6 Entities
const val DATA_MIGRATION_VERSION_1 ="""
    [
  {
    "entity": "products",
    "type": "writable",
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
        "name": "mv_size",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "mv_variant",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "volume",
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
        "name": "weight",
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
            "type": "writable",
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
                    "entity": "categories_lots",
                    "type": "writable",
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
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
            "entity": "product_milk_sales",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
                "name": "mv_product_consume",
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
            "entity": "product_locations",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
"delete_on_cascade": true
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
    "entity": "products",
    "type": "writable",
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
        "name": "mv_size",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "mv_variant",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "volume",
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
        "name": "weight",
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
            "type": "writable",
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
                "name": "product_id",
                "version": 2,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
                    "entity": "categories_lots",
                    "type": "writable",
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
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
            "entity": "product_milk_sales",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
                "name": "mv_product_consume",
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
            "entity": "product_locations",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
    "entity": "products",
    "type": "writable",
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
        "name": "mv_size",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "mv_variant",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "volume",
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
        "name": "weight",
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
            "entity": "products_metadata",
            "type": "writable",
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
                "name": "product_id",
                "version": 3,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
            "type": "writable",
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
                "name": "product_id",
                "version": 2,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
"delete_on_cascade": true
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
                    "entity": "categories_lots",
                    "type": "writable",
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
                        "name": "product_id",
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
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
            "entity": "product_milk_sales",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
                "name": "mv_product_consume",
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
            "entity": "product_locations",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
    "entity": "measures",
    "type": "writable",
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
        "name": "nullable",
        "version": 1,
        "type": "int",
        "nullable": true
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
    "entity": "product_breeds",
    "type": "readable",
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

const val DATA_MIGRATION_INITIAL_DATA_TASK = """
    [
  {
    "entity": "measures",
    "type": "writable",
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
    "entity": "products",
    "type": "writable",
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
        "name": "mv_size",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "mv_variant",
        "version": 1,
        "type": "uuid",
        "nullable": false
      },
      {
        "name": "volume",
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
        "name": "weight",
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
            "entity": "products_metadata",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
            "entity": "categories",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
                "nullable": false,
                "options": [
                  "m",
                  "f",
                  "ud"
                ]
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
                "name": "purpose",
                "version": 1,
                "type": "enum",
                "nullable": false,
                "options": [
                  "1",
                  "2",
                  "3",
                  "4"
                ]
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
                "nullable": false,
                "options": [
                  "0",
                  "1",
                  "2"
                ]
              },
              {
                "name": "stage",
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
                "name": "reproductive_status",
                "version": 1,
                "type": "enum",
                "nullable": false,
                "options": [
                  "1",
                  "2",
                  "3"
                ]
              },
              {
                "name": "health_status",
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
                "name": "inside",
                "version": 1,
                "type": "boolean",
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
            "entity": "product_locations",
            "type": "writable",
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
                "name": "product_id",
                "version": 1,
                "type": "uuid",
                "nullable": false,
                "linked_entity": "products",
                "delete_on_cascade": true
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
  }
]
"""

const val DATA_SYNC_INITIAL_DATA_TASK = """
    [
    {
        "entity": "measures",
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
        "entity": "products",
        "data": {
            "id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
            "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
            "sync_hash": "e3d47ef233c70cc1819b8cf63818d7f79c3dc8d54d90c8241b76eae67be7254d",
            "sync_created_at": 1725051913,
            "sync_updated_at": 1725051913,
            "name": "Finca Los Alpinos",
            "destination": "1",
            "mv_size": "5701514f-fd5c-4417-b3d9-6fdcd8ee746b",
            "mv_variant": "4635a0a7-8548-4327-b74d-9ca88a6ccf90",
            "volume": "L",
            "weight": "kg",
            "type": "1",
            "_metadata": [],
            "_lots": [],
            "_categories": [
                {
                    "entity": "categories",
                    "data": {
                        "id": "00aedddd-325d-4472-a04c-27e57f5d6018",
                        "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
                        "sync_hash": "d2b4f095f8634c7c19ec7a173fa3ce84830e0a20f20d75a8749cc7a5f6cfbb83",
                        "sync_created_at": 1725051913,
                        "sync_updated_at": 1725051913,
                        "product_id": "797b62cb-d6f5-436b-9ae0-6657d0ae979a",
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
    },
    {
        "entity": "products",
        "data": {
            "id": "7d00764c-9660-4f32-b9c0-00bc2ef0f6fa",
            "sync_owner_id": "5160ea14-8676-3881-9b93-0859a7f59431",
            "sync_hash": "2a1fbbb73aa1d37cb9e36874ea900034e16de18263960fde38b01578380a80a9",
            "sync_created_at": 1725281682,
            "sync_updated_at": 1725281682,
            "name": "Finca Maracana",
            "destination": "1",
            "mv_size": "ec8f99e4-82c9-496f-9ba8-4c39585daa27",
            "mv_variant": "03b0fe9c-b06f-489f-8148-8301bec3f89a",
            "volume": "L",
            "weight": "kg",
            "type": "1",
            "_metadata": [],
            "_lots": [],
            "_categories": [],
            "_irons": [],
            "_milkSales": []
        }
    }
]
"""


const val MOCK_RESPONSE_FILE_INFO = """
{
    "id": "3849a6d2-d8ed-4431-9502-717b8b4a888f",
    "url": "http://dev.api.controlganadero.com/storage/horus/upload/pending/5160ea14-8676-3881-9b93-0859a7f59431/3849a6d2-d8ed-4431-9502-717b8b4a888f.png",
    "mime_type": "image/png",
    "status": "0"
}
"""

const val MOCK_RESPONSE_FILES_INFO = """
    [
    {
        "id": "68b1e65a-7626-4490-8ee4-0427adf43688",
        "url": "http://dev.api.controlganadero.com/storage/horus/upload/pending/5160ea14-8676-3881-9b93-0859a7f59431/68b1e65a-7626-4490-8ee4-0427adf43688.jpg",
        "mime_type": "image/jpeg",
        "status": "0"
    },
    {
        "id": "91205add-c035-49c5-9134-8f3c770d336a",
        "url": "http://dev.api.controlganadero.com/storage/horus/upload/5160ea14-8676-3881-9b93-0859a7f59431/farms/7d00764c-9660-4f32-b9c0-00bc2ef0f6fb/animals/be655fd3-1549-49ce-9829-69a9fa345607/animal_images/c72594b4-d0df-4b20-8d16-ebc1ca1bd119/91205add-c035-49c5-9134-8f3c770d336a.jpg",
        "mime_type": "image/jpeg",
        "status": "1"
    }
]
"""

const val MOCK_RESPONSE_UPLOAD_FILE = MOCK_RESPONSE_FILE_INFO

const val MOCK_RESPONSE_POST_START_SYNC = """
{
    "sync_id": "fb2c8bb1-a8ef-49fa-a8dc-69133ff3567e"
}
"""

const val MOCK_RESPONSE_GET_SYNC_STATUS = """
{
  "id": "sync-123",
  "user_id": "user-456",
  "status": "completed",
  "result_at": 1725044885,
  "download_url": "https://cg-storage.nyc3.digitaloceanspaces.com/horus/upload/sync/ef2ef583-2f61-4386-bea2-ab5d3de8529b.json?response-content-type=application%2Fjson&response-content-disposition=attachment%3B%20filename%3D%22ef2ef583-2f61-4386-bea2-ab5d3de8529b.json%22&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=DO801YJBHWCCPVJ3H7FN%2F20250727%2Fnyc3%2Fs3%2Faws4_request&X-Amz-Date=20250727T201717Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3600&X-Amz-Signature=fe5da1c891d0577fbbc4db7f2fca2f2f5ae6607b4c74852646778a21c9f56461",
  "checkpoint": "checkpoint-789"
}
"""

const val MOCK_RESPONSE_SYNC_DATA_FILE = """{"entity":"products","data":{"id":"7d00764c-9660-4f32-b9c0-00bc2ef0f6fa","sync_owner_id":"5160ea14-8676-3881-9b93-0859a7f59431","sync_hash":"2a1fbbb73aa1d37cb9e36874ea900034e16de18263960fde38b01578380a80a9","sync_created_at":1725281682,"sync_updated_at":1725281682,"name":"Finca Maracana","destination":"1","mv_size":"ec8f99e4-82c9-496f-9ba8-4c39585daa27","mv_variant":"03b0fe9c-b06f-489f-8148-8301bec3f89a","volume":"L","weight":"kg","type":"1"}}
{"entity":"products","data":{"id":"797b62cb-d6f5-436b-9ae0-6657d0ae979a","sync_owner_id":"5160ea14-8676-3881-9b93-0859a7f59431","sync_hash":"e3d47ef233c70cc1819b8cf63818d7f79c3dc8d54d90c8241b76eae67be7254d","sync_created_at":1725051913,"sync_updated_at":1725051913,"name":"FincaLosAlpinos","destination":"1","mv_size":"5701514f-fd5c-4417-b3d9-6fdcd8ee746b","mv_variant":"4635a0a7-8548-4327-b74d-9ca88a6ccf90","volume":"L","weight":"kg","type":"1"}}
{"entity":"categories","data":{"id":"00aedddd-325d-4472-a04c-27e57f5d6018","sync_owner_id":"5160ea14-8676-3881-9b93-0859a7f59431","sync_hash":"d2b4f095f8634c7c19ec7a173fa3ce84830e0a20f20d75a8749cc7a5f6cfbb83","sync_created_at":1725051913,"sync_updated_at":1725051913,"product_id":"797b62cb-d6f5-436b-9ae0-6657d0ae979a","name":"Camila","code":"9012345","gender":"f","type":"1","purpose":"2","sale_status":"0","stage":"4","reproductive_status":"1","health_status":"1","inside":1}}
{"entity":"measures","data":{"id":"3093a07a-543b-336b-9ca8-4c3bf207aeb5","sync_owner_id":"5160ea14-8676-3881-9b93-0859a7f59431","sync_hash":"89424a4c2efa23f3b6a0caa03bfdcf66dd3d6d31c0559c8c286f82fcd3798f6d","sync_created_at":1725044885,"sync_updated_at":1725044885,"measure":"w","unit":"kg","value":10}}
"""
