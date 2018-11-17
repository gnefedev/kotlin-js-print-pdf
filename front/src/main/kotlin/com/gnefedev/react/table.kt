package com.gnefedev.react

import kotlinx.html.HTMLTag
import kotlinx.html.TABLE
import kotlinx.html.TBODY
import kotlinx.html.TD
import kotlinx.html.TFOOT
import kotlinx.html.TH
import kotlinx.html.THEAD
import kotlinx.html.TR
import kotlinx.html.TagConsumer
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tfoot
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.table
import react.dom.tbody
import react.dom.td
import react.dom.tfoot
import react.dom.th
import react.dom.thead
import react.dom.tr

fun RBuilder.reactTable(block: Table.() -> Unit) =
            Table().apply(block).visit(this)

fun htmlTable(block: Table.() -> Unit) = StringBuilder().appendHTML()
    .apply {
        Table().apply(block).visit(this)
    }
    .finalize()
    .toString()

@DslMarker
annotation class StyledTableMarker

@StyledTableMarker
interface Tag

abstract class ParentTag<T : HTMLTag> : Tag {
    val tags: MutableList<TagWithParent<*, T>> = mutableListOf()

    protected fun RDOMBuilder<T>.withChildren() {
        tags.onEach { it.appendToParent(this) }
    }

    protected fun T.withChildren() {
        tags.onEach { it.appendToParent(this) }
    }
}

class TagWithParent<T : Tag, P : HTMLTag>(
    private val tag: T,
    val htmlAppender: (T, P) -> Unit,
    val reactAppender: (T, RDOMBuilder<P>) -> Unit
) {
    fun appendToParent(parent: P) = htmlAppender(tag, parent)
    fun appendToParent(parent: RDOMBuilder<P>) = reactAppender(tag, parent)
}

class Table : ParentTag<TABLE>() {
    fun visit(builder: RBuilder) {
        builder.table {
            withChildren()
        }
    }

    fun visit(builder: TagConsumer<*>) {
        builder.table {
            style = "page-break-inside: avoid"
            withChildren()
        }
    }
}

class THead : ParentTag<THEAD>() {
    fun visit(builder: RDOMBuilder<TABLE>) {
        builder.thead {
            withChildren()
        }
    }

    fun visit(builder: TABLE) {
        builder.thead {
            withChildren()
        }
    }
}

fun Table.thead(block: THead.() -> Unit) {
    tags += TagWithParent(
        THead().also(block),
        THead::visit,
        THead::visit
    )
}

fun THead.tr(block: Tr.() -> Unit) {
    tags += TagWithParent(
        Tr("").also(block),
        Tr::visit,
        Tr::visit
    )
}

fun TFooter.tr(block: Tr.() -> Unit) {
    tags += TagWithParent(
        Tr("").also(block),
        Tr::visit,
        Tr::visit
    )
}

fun Table.tbody(block: TBody.() -> Unit) {
    tags += TagWithParent(
        TBody().also(block),
        TBody::visit,
        TBody::visit
    )
}

class TBody : ParentTag<TBODY>() {
    fun visit(builder: RDOMBuilder<TABLE>) {
        builder.tbody {
            withChildren()
        }
    }

    fun visit(builder: TABLE) {
        builder.tbody {
            withChildren()
        }
    }
}


fun Table.tfoot(block: TFooter.() -> Unit) {
    tags += TagWithParent(
        TFooter().also(block),
        TFooter::visit,
        TFooter::visit
    )
}

class TFooter : ParentTag<TFOOT>() {
    fun visit(builder: RDOMBuilder<TABLE>) {
        builder.tfoot {
            withChildren()
        }
    }

    fun visit(builder: TABLE) {
        builder.tfoot {
            withChildren()
        }
    }
}

class Style {
    var border: String? = null
    var borderColor: String? = null
    var width: String? = null
    var padding: String? = null
    var background: String? = null

    operator fun invoke(callback: Style.() -> Unit) {
        callback()
    }

    fun toHtmlStyle(): String = properties
        .map { it.html to it.property(this) }
        .filter { (_, value) -> value != null }
        .joinToString("; ") { (name, value) -> "$name: $value" }

    fun toReactStyle(): String {
        val result = js("{}")
        properties
            .map { it.react to it.property(this) }
            .filter { (_, value) -> value != null }
            .forEach { (name, value) -> result[name] = value.toString() }
        return result.unsafeCast<String>()
    }

    class StyleProperty(
        val html: String,
        val react: String,
        val property: Style.() -> Any?
    )

    companion object {
        val properties = listOf(
            StyleProperty("border", "border") { border },
            StyleProperty("border-color", "borderColor") { borderColor },
            StyleProperty("width", "width") { width },
            StyleProperty("padding", "padding") { padding },
            StyleProperty("background", "background") { background }
        )
    }
}


fun TBody.tr(block: Tr.() -> Unit) {
    tags += TagWithParent(
        Tr("").also(block),
        Tr::visit,
        Tr::visit
    )
}

class Tr(
    val classes: String?
) : ParentTag<TR>() {
    fun visit(builder: RDOMBuilder<THEAD>) {
        builder.tr(classes) {
            withChildren()
        }
    }

    fun visit(builder: THEAD) {
        builder.tr(classes) {
            withChildren()
        }
    }

    fun visit(builder: RDOMBuilder<TFOOT>) {
        builder.tr(classes) {
            withChildren()
        }
    }

    fun visit(builder: TFOOT) {
        builder.tr(classes) {
            withChildren()
        }
    }

    fun visit(builder: RDOMBuilder<TBODY>) {
        builder.tr(classes) {
            withChildren()
        }
    }

    fun visit(builder: TBODY) {
        builder.tr(classes) {
            withChildren()
        }
    }
}

fun Tr.th(classes: String = "", block: Th.() -> Unit) {
    tags += TagWithParent(
        Th(classes).also(block),
        Th::visit,
        Th::visit
    )
}

fun Tr.td(classes: String? = null, block: Td.() -> Unit) {
    tags += TagWithParent(
        Td(classes).also(block),
        Td::visit,
        Td::visit
    )
}

abstract class TableCell<T : HTMLTag> : ParentTag<T>() {
    val attrs = Attributes()

    operator fun String.unaryPlus() {
        tags += TagWithParent<TextTag, T>(
            TextTag(this),
            TextTag::visit,
            TextTag::visit
        )
    }

    data class Attributes(
        var colSpan: Int? = null,
        var rowSpan: Int? = null,
        var style: Style = Style()
    )
}

class Td(private val classes: String?) : TableCell<TD>() {
    fun visit(builder: RDOMBuilder<TR>) {
        builder.td(classes) {
            this@Td.attrs.colSpan?.let { attrs.colSpan = it.toString() }
            this@Td.attrs.rowSpan?.let { attrs.rowSpan = it.toString() }
            attrs.style = this@Td.attrs.style.toReactStyle()
            withChildren()
        }
    }

    fun visit(builder: TR) {
        builder.td(classes) {
            this@Td.attrs.colSpan?.let { colSpan = it.toString() }
            this@Td.attrs.rowSpan?.let { rowSpan = it.toString() }
            style = this@Td.attrs.style.toHtmlStyle()
            withChildren()
        }
    }
}

class Th(
    val classes: String = ""
) : TableCell<TH>() {
    fun visit(builder: RDOMBuilder<TR>) {
        builder.th(classes = classes) {
            this@Th.attrs.colSpan?.let { attrs.colSpan = it.toString() }
            this@Th.attrs.rowSpan?.let { attrs.rowSpan = it.toString() }
            attrs.style = this@Th.attrs.style.toReactStyle()
            withChildren()
        }
    }

    fun visit(builder: TR) {
        builder.th(classes = classes) {
            this@Th.attrs.colSpan?.let { colSpan = it.toString() }
            this@Th.attrs.rowSpan?.let { rowSpan = it.toString() }
            style = this@Th.attrs.style.toHtmlStyle()
            withChildren()
        }
    }
}

class TextTag(
    private val text: String
) : Tag {
    fun visit(builder: HTMLTag) {
        builder.text(text)
    }

    fun visit(builder: RDOMBuilder<HTMLTag>) {
        builder.apply { +text }
    }
}

class ReactTag<T : HTMLTag>(
    private val block: RBuilder.() -> Unit = {}
) : Tag {
    private var htmlAppender: (T) -> Unit = {}

    infix fun html(block: (T).() -> Unit) {
        htmlAppender = block
    }

    fun visit(builder: T) {
        htmlAppender(builder)
    }

    fun visit(builder: RDOMBuilder<HTMLTag>) {
        builder.apply(block)
    }
}

fun <T : HTMLTag> ParentTag<T>.react(block: RBuilder.() -> Unit): ReactTag<T> {
    val reactTag = ReactTag<T>(block)
    tags += TagWithParent<ReactTag<T>, T>(
        reactTag,
        ReactTag<T>::visit,
        ReactTag<T>::visit
    )
    return reactTag
}
