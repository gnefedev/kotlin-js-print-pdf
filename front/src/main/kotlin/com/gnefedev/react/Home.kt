package com.gnefedev.react

import com.gnefedev.react.Home.State
import kotlinext.js.js
import kotlinx.html.ButtonType
import kotlinx.html.TagConsumer
import kotlinx.html.onChange
import kotlinx.html.onClick
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.fetch.RequestInit
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.button
import react.dom.div
import react.dom.input
import react.dom.table
import react.dom.tbody
import react.dom.td
import react.dom.th
import react.dom.thead
import react.dom.tr
import react.setState
import kotlin.browser.window

class Home : RComponent<RProps, State>() {
  override fun RBuilder.render() {
    div {
      button(type = ButtonType.button) {
        +"Печать"
        attrs.onClick = eventHandler {
          printHtml(StringBuilder()
            .appendHTML()
            .apply {
              renderHtmlTable(state.search)
            }
            .finalize().toString()
          )
        }
      }
      renderReactTable(state.search) {
        setState {
          search = it
        }
      }

      +"CrossPlatform version:"
      button(type = ButtonType.button) {
        +"Печать"
        attrs.onClick = eventHandler {
          printHtml(htmlTable {
            renderCrossPlatformTable(state.search) {
              setState {
                search = it
              }
            }
          })
        }
      }
      reactTable {
        renderCrossPlatformTable(state.search) {
          setState {
            search = it
          }
        }
      }
    }
  }

  class State : RState {
    var search: String? = null
  }
}

fun RBuilder.renderReactTable(
  search: String?,
  onChangeSearch: (String?) -> Unit
) {
  table {
    thead {
      tr {
        th {
          attrs.colSpan = "2" //(1)
          attrs.style = js {
            border = "solid"
            borderColor = "red"
          } //(2)
          +"Поиск:"
          search(search,
            onChangeSearch) //(3)
        }
      }
      tr {
        th { +"Имя" }
        th { +"Фамилия" }
      }
    }
    tbody {
      tr {
        td { +"Иван" }
        td { +"Иванов" }
      }
      tr {
        td { +"Петр" }
        td { +"Петров" }
      }
    }
  }
}

fun TagConsumer<*>.renderHtmlTable(
  search: String?

) {
  table {
    thead {
      tr {
        th {
          colSpan = "2" //(1)
          style = """
            border: solid;
            border-color: red;
          """ //(2)
          +"Поиск: "
          +(search?:"") //(3)

        }
      }
      tr {
        th { +"Имя" }
        th { +"Фамилия" }
      }
    }
    tbody {
      tr {
        td { +"Иван" }
        td { +"Иванов" }
      }
      tr {
        td { +"Петр" }
        td { +"Петров" }
      }
    }
  }
}

fun Table.renderCrossPlatformTable(search: String?, onChangeSearch: (String?) -> Unit) {
  thead {
    tr {
      th {
        attrs.colSpan = 2
        attrs.style {
          border = "solid"
          borderColor = "red"
        }
        +"Поиск:"
        react {
          search(search, onChangeSearch)
        } html {
          +(search?:"")
        }
      }
    }
    tr {
      th { +"Имя" }
      th { +"Фамилия" }
    }
  }
  tbody {
    tr {
      td { +"Иван" }
      td { +"Иванов" }
    }
    tr {
      td { +"Петр" }
      td { +"Петров" }
    }
  }
}

private fun RBuilder.search(
  search: String?,
  onChangeSearch: (String?) -> Unit
) {
  input {
    attrs {
      value = search ?: ""
      onChange = eventHandler {
        onChangeSearch((it.target as HTMLInputElement).value)
      }
    }
  }
}

fun eventHandler(handler: (Event) -> Unit): String {
  return handler.asDynamic().unsafeCast<String>()
}

fun printHtml(html: String) {
  window.fetch(
    "api/pdf/print", init = RequestInit(
      method = "POST",
      body = html,
      cache = undefined,
      redirect = undefined,
      credentials = undefined,
      headers = undefined,
      mode = undefined,
      referrerPolicy = undefined,
      integrity = undefined
    )
  ).then {
    it.blob().then { saveAs(it, "printed.pdf") }
  }
}
