package com.example

import org.intellij.lang.annotations.Language

@Language("JSON")
val response = """{
    "data": {
      "viewer": {
        "__typename": "Viewer", 
        "books": [
          {
            "__typename": "Book",
            "id": "id=1",
            "name": "ok-2"
          }
        ],
        "justAField": "test"
      }
    }
  }"""
