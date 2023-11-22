package com.example

import org.intellij.lang.annotations.Language

@Language("JSON")
val responseWithData = """{
    "data": {
      "viewer": {
        "__typename": "Viewer",
        "id": "viewerId",
        "similarBook": {
            "__typename": "Book",
            "id": "fixture-id",
            "name": "ok-2"
          }
      }
    }
  }"""

@Language("JSON")
val responseWithNull = """{
    "data": {
      "viewer": {
        "__typename": "Viewer", 
        "id": "viewerId",
        "similarBook": null
      }
    }
  }"""
