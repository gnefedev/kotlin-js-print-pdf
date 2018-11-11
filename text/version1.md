Одно кольцо, чтобы управлять ими всеми, одно кольцо, чтобы найти их, одно кольцо, чтобы собрать их всех, в темноте соединить их.

Нельзя просто взять и распечатать страницу на React: есть разделители страниц, поля для ввода. Кроме того, хочется один раз написать
рендеринг, чтобы он генерил как ReactDom, так и обычный html, который можно сконвертить в pdf.
Самое сложное, что у React свой dsl, а у html свой. Как решить эту проблему? Написать свой.
Чуть не забыл, всё это будет написано на Kotlin, так что, на самом деле, это статья о Kotlin dsl.

В качестве примера, будем работать с таблицами. Так выглядит отрисовка таблицы на React и html:
```kotlin
fun RBuilder.renderReactTable() {
    table { 
        thead { 
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

fun TagConsumer<*>.renderHtmlTable() {
    table {
        thead {
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
```

Пока выглядит абсалютно одинаково. За исключением большого но: это разные dsl с разными консьюмерами и разным api.

Вот так выглядит вызов одного и другого варианта:
```kotlin
fun renderReactTableWrap(): ReactElement? = buildElement { 
    renderReactTable()
}

fun renderHtmlTableWrap(): String = StringBuilder()
    .appendHTML()
    .apply { renderHtmlTable() }
    .finalize().toString()
```

Итак у нас есть html дерево и два способа его обработки - будем реализовывать visitor, но немного в непривычном варианте:
у нас не будет интерфейсов. Почему будет видно позднее.

В качестве оснвных единиц будут выступать Tag, ParentTag и TagWithParent. Зачем нужен отдельный TagWithParent?
Проблема в том, что dsl для html очень строг при компиляции. Если в React можно вызывать td откуда угодно, хоть из div, 
то в случае html его можно вызвать только из контекста tr. Поэтому нам придется везде протаскивать контекст для компиляции.

```kotlin
interface Tag

abstract class ParentTag<T : HTMLTag> : Tag {
    val tags: MutableList<TagWithParent<*, T>> = mutableListOf()

    protected fun RDOMBuilder<T>.withChildren() { ... }
    protected fun T.withChildren() { ... }
}

class TagWithParent<T : Tag, P : HTMLTag>(
    private val tag: T,
    val htmlAppender: (T, P) -> Unit,
    val reactAppender: (T, RDOMBuilder<P>) -> Unit
)
```

Tag - просто маркировочный интерфейс.

ParentTag дженифицирован по html тегу из api Kotlin (слава Богу, он переиспользуется в html и react dsl). 

TagWithParent хранит сам тег и две функции, которые вставляют его в родетеля в двух вариантах api (вот и контекст для компиляции).

Большая часть тегов при этом пишется примерно одинвокво. Вот пример THead
```kotlin
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
```

Теперь можно объяснить, почему не использовались интерфейсы. Проблема в том, что tr может быть вставлен как в 
thead так и в tbody. Выразить это в рамках одного интерфеса мне не удалось, вышло четыре перегрузки одной функции:
```kotlin
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
```

Есть ещё одна проблема: в react и html api по-разному заполняется style. Если в html это обычный css в виде строки, то
в react это js объект, названия полей у которого немного отличаются от стандартных css в силу ограничений js. 

/*такой синтаксис позволяет сделать вызов такого вида:
    attrs.style {
        width = "150px"
    }
    */
