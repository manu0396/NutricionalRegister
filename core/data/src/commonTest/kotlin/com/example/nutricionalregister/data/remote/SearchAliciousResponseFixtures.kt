package com.example.nutricionalregister.data.remote

internal object SearchAliciousResponseFixtures {
    val oneProduct =
        """
        {
          "hits": [
            {
              "code": "0012345678905",
              "product_name": "Oat drink",
              "brands": "Example Foods",
              "quantity": "1 L",
              "nutrition_data_per": "100ml",
              "nutriments": {
                "energy-kcal_100g": 42.0,
                "fat_100g": 1.2,
                "fat_unit": "g",
                "proteins_100g": 0.8
              }
            }
          ],
          "page": 1,
          "page_size": 20,
          "count": 1
        }
        """.trimIndent()

    val noProducts =
        """
        {
          "hits": [],
          "page": 1,
          "page_size": 20,
          "count": 0
        }
        """.trimIndent()

    val mixedProducts =
        """
        {
          "hits": [
            { "code": "broken", "product_name": "   " },
            { "code": "00042", "product_name": "Beans", "nutriments": { "proteins_100g": 0 } },
            { "product_name": "Missing code" }
          ]
        }
        """.trimIndent()

    val arrayBrandsProduct =
        """
        {
          "hits": [
            {
              "code": "0099999999999",
              "product_name": "Oat drink natural",
              "brands": ["Lima", "Oatly"],
              "quantity": ["1 L", "33 cl"],
              "nutriscore_grade": "unknown",
              "nutriments": { "proteins_100g": 7.68, "fat_100g": 1.2 }
            }
          ],
          "page": 1,
          "page_size": 20,
          "count": 1
        }
        """.trimIndent()

    val emptyArrayBrandsProduct =
        """
        {
          "hits": [
            {
              "code": "0088888888888",
              "product_name": "Plain water",
              "brands": [],
              "quantity": []
            }
          ]
        }
        """.trimIndent()
}
