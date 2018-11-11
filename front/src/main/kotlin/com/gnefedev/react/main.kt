package com.gnefedev.react

import react.dom.render
import react.router.dom.browserRouter
import react.router.dom.route
import react.router.dom.switch
import kotlin.browser.document
import kotlin.browser.window

fun main(args: Array<String>) {
    window.onload = {
        render(document.getElementById("react")!!) {
            browserRouter {
                switch {
                    route("/", component = Home::class, exact = true)
                }
            }
        }
    }
}
