package com.example

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class Test : TestBase() {

  @Test
  internal fun `caching by path`(): Unit = runBlocking {
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "clientName": "First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "clientName": "Second ClientName"
        }
      }
    ]
  }
}"""
    )

    val (firstQuery, firstJob) = watch(PendingActionsListWithJobWithClientNameQuery())

    val firstQuery1 = firstQuery.receive()

    cacheString()

    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "name": "First Job",
          "clientName": "New First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "name": "Second Job",
          "clientName": "New Second ClientName"
        }
      }
    ]
  }
}"""
    )

    val (secondQuery, secondJob) = watch(PendingActionsListWithJobWithNameAndClientNameQuery())
    val secondQuery1 = secondQuery.receive()
    val firstQuery2 = firstQuery.receive()

    cacheString()

    mockWebServer.enqueue(
      """{
  "data": {
    "home": {
      "__typename": "Home",
      "pendingActions": [
        {
          "__typename": "PendingAction",
          "job": {
            "__typename": "Job",
            "name": "Home First Job",
            "clientName": "Home First ClientName"
          }
        },
        {
          "__typename": "PendingAction",
          "job": {
            "__typename": "Job",
            "name": "Home Second Job",
            "clientName": "Home New Second ClientName"
          }
        }
      ]
    }
  }
}"""
    )

    val (thirdQuery, thirdJob) = watch(HomePendingActionsQuery())

    val thirdQuery1 = thirdQuery.receive()
    firstQuery.assertNoEmission()
    secondQuery.assertNoEmission()

    cacheString()

    firstJob.cancel()
    secondJob.cancel()
    thirdJob.cancel()
  }

  @Test
  internal fun `caching by id`(): Unit = runBlocking {
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "clientName": "First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "clientName": "Second ClientName"
        }
      }
    ]
  }
}"""
    )

    val (firstQuery, firstJob) = watch(PendingActionsListWithIdAndClientNameQuery())

    val firstQuery1 = firstQuery.receive()

    cacheString()

    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "name": "First Job",
          "clientName": "New First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "name": "Second Job",
          "clientName": "New Second ClientName"
        }
      }
    ]
  }
}"""
    )

    val (secondQuery, secondJob) = watch(PendingActionsListWithIdAndNameAndClientNameQuery())
    val secondQuery1 = secondQuery.receive()
    val firstQuery2 = firstQuery.receive()

    cacheString()

    mockWebServer.enqueue(
      """{
  "data": {
    "home": {
      "__typename": "Home",
      "pendingActions": [
        {
          "__typename": "PendingAction",
          "job": {
            "__typename": "Job",
            "id": "job-1",
            "name": "Home First Job",
            "clientName": "Home First ClientName"
          }
        },
        {
          "__typename": "PendingAction",
          "job": {
            "__typename": "Job",
            "id": "job-2",
            "name": "Home Second Job",
            "clientName": "Home New Second ClientName"
          }
        }
      ]
    }
  }
}"""
    )

    val (thirdQuery, thirdJob) = watch(HomePendingActionsWithIdQuery())

    val thirdQuery1 = thirdQuery.receive()
    val firstQuery3 = firstQuery.receive()
    val secondQuery2 = secondQuery.receive()

    cacheString()

    firstJob.cancel()
    secondJob.cancel()
    thirdJob.cancel()
  }

  @Test
  internal fun `disjoint queries additional network request`(): Unit = runBlocking {
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "name": "First Name",
          "clientName": "First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "name": "Second Name",
          "clientName": "Second ClientName"
        }
      }      
    ]
  }
}"""
    )
    val (firstQuery, firstJob) = watch(PendingActionsListWithIdAndNameAndClientNameQuery())

    val firstQuery1 = firstQuery.receive()

    //language=JSON
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "clientName": "First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "clientName": "Second ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-3",
          "clientName": "Third ClientName"
        }
      }
    ]
  }
}"""
    )

    val (secondQuery, secondJob) = watch(PendingActionsListWithIdAndClientNameQuery(), networkOnly = true)

    val secondQuery1 = secondQuery.receive()

    cacheString()

    firstJob.cancel()
    secondJob.cancel()
  }

  @Test
  internal fun `disjoint queries with empty list on smaller response`(): Unit = runBlocking {
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "name": "First Name",
          "clientName": "First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "name": "Second Name",
          "clientName": "Second ClientName"
        }
      }      
    ]
  }
}"""
    )
    val (firstQuery, firstJob) = watch(PendingActionsListWithIdAndNameAndClientNameQuery())

    val firstQuery1 = firstQuery.receive()

    //language=JSON
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": []
  }
}"""
    )

    val (secondQuery, secondJob) = watch(PendingActionsListWithIdAndClientNameQuery(), networkOnly = true)

    val secondQuery1 = secondQuery.receive()
    val firstQuery2 = firstQuery.receive()

    cacheString()

    firstJob.cancel()
    secondJob.cancel()
  }

  @Test
  internal fun `disjoint queries with smaller list on smaller response`(): Unit = runBlocking {
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "name": "First Name",
          "clientName": "First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "name": "Second Name",
          "clientName": "Second ClientName"
        }
      }      
    ]
  }
}"""
    )
    val (firstQuery, firstJob) = watch(PendingActionsListWithIdAndNameAndClientNameQuery())

    val firstQuery1 = firstQuery.receive()

    //language=JSON
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "clientName": "Second ClientName"
        }
      }
    ]
  }
}"""
    )

    val (secondQuery, secondJob) = watch(PendingActionsListWithIdAndClientNameQuery(), networkOnly = true)

    val secondQuery1 = secondQuery.receive()
    val firstQuery2 = firstQuery.receive()

    assert(firstQuery2.data!!.pendingActions.size == 1)

    cacheString()

    firstJob.cancel()
    secondJob.cancel()
  }

  @Test
  internal fun `disjoint queries with updated list on smaller response`(): Unit = runBlocking {
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "name": "First Name",
          "clientName": "First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "name": "Second Name",
          "clientName": "Second ClientName"
        }
      }      
    ]
  }
}"""
    )
    val (firstQuery, firstJob) = watch(PendingActionsListWithIdAndNameAndClientNameQuery())

    val firstQuery1 = firstQuery.receive()

    //language=JSON
    mockWebServer.enqueue(
      """{
  "data": {
    "pendingActions": [
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-1",
          "clientName": "Updated First ClientName"
        }
      },
      {
        "__typename": "PendingAction",
        "job": {
          "__typename": "Job",
          "id": "job-2",
          "clientName": "Updated Second ClientName"
        }
      }      
    ]
  }
}"""
    )

    val (secondQuery, secondJob) = watch(PendingActionsListWithIdAndClientNameQuery(), networkOnly = true)

    val secondQuery1 = secondQuery.receive()
    val firstQuery2 = firstQuery.receive()

    assert(firstQuery2.data!!.pendingActions[0].job.clientName == "Updated First ClientName")
    assert(firstQuery2.data!!.pendingActions[1].job.clientName == "Updated Second ClientName")

    cacheString()

    firstJob.cancel()
    secondJob.cancel()
  }
}

