Одно кольцо, чтобы управлять ими всеми, одно кольцо, чтобы найти их, одно кольцо, чтобы собрать их всех, в темноте соединить их.

Нельзя просто взять и распечатать страницу на React: есть разделители страниц, поля для ввода. Кроме того, хочется один раз написать
рендеринг, чтобы он генерил как ReactDom, так и обычный html, который можно сконвертить в pdf.
Самое сложное, что у React свой dsl, а у html свой. Как решить эту проблему? Написать свой.
Чуть не забыл, всё это будет написано на Kotlin, так что, на самом деле, это статья о Kotlin dsl.

В качестве примера, возьмем таблицу с поисковой сторокой в заговоке. Так выглядит отрисовка таблицы на React и html:
<table>
<tr>
<th>
react
</th>
<th>
html
</th>
</tr>
<tr>
<td>
<source lang="kotlin">
fun RBuilder.renderReactTable(
  search: String,
  onChangeSearch: (String) -> Unit
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
</source>
</td>
<td>
<source lang="kotlin">
fun TagConsumer<*>.renderHtmlTable(
  search: String

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
</source>
</td>
</tr>
</table>

Для начала разберемся в чем разница:
<ul>
<li>В html версии <code>style</code> и <code>colSpan</code> присваются на верхнем уровне, в react - на вложенном объекте attr</li>
<li>По-разному заполняется style. Если в html это обычный css в виде строки, то в react это js объект, названия полей у которого немного отличаются от стандартных css в силу ограничений js.</li>
<li>В react версии для поиска мы используем input, в html просто выводим текст. Это уже исходит из постановки задачи</li>
</ul>

И самое важное: это разные dsl с разными консьюмерами и разным api. Для компилятора они абсалютно не похожи. Напрямую срестить их невозможно,
поэтому придется писать прослойку, которая будет выглядеть почти также, но сможет формировать как reactDom, так и html.

Итак у нас есть html дерево и два способа его обработки - будем реализовывать composite и visitor, но немного в непривычном варианте:
у нас не будет интерфейса для visitor. Почему будет видно позднее.

В качестве оснвных единиц будут выступать Tag, ParentTag и TagWithParent. Зачем нужен отдельный TagWithParent?
Проблема в том, что dsl для html очень строг при компиляции. Если в React можно вызывать td откуда угодно, хоть из div, 
то в случае html его можно вызвать только из контекста tr. Поэтому нам придется везде протаскивать контекст для компиляции в виде generic.

<source lang="kotlin">
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
</source>

Tag - просто маркировочный интерфейс.

ParentTag дженифицирован по html тегу из api Kotlin (слава Богу, он переиспользуется в html и react dsl). 

TagWithParent хранит сам тег и две функции, которые вставляют его в родетеля в двух вариантах api (вот и контекст для компиляции).

Большая часть тегов при этом пишется примерно одинвокво. Вот пример THead
<source lang="kotlin">
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
</source>

Теперь можно объяснить, почему не использовался интерфейс для visitor. Проблема в том, что tr может быть вставлен как в 
thead так и в tbody. Выразить это в рамках одного интерфеса мне не удалось, вышло четыре перегрузки одной функции:
<source lang="kotlin">
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
</source>

Теперь надо разобраться с местами, которые отличаются в api html и react. Небольшая разница с colSpan решается сама собой, 
а вот различие в формировании style - посложнее. Можно было бы попробовать автоматически приводить camelCase к написанию
через дефис и оставить как в react api, но всегда ли оно будет работать - не знаю. Поэтому написал ещё одну прослойку:

Класс Style с набором полей, который умеет конвертиться в два варианта style
<source lang="kotlin">
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
</source>

Я немного схитрил и позволил вот такое использование:
<source lang="kotlin">
th {
  attrs.style {
    border = "solid"
    borderColor = "red"
  }
}
</source>
Как это выходит: в поле style по-умолчанию уже лежит пустой Style(). Если определить operator fun invoke, то объект
можно использовать как функцию, передавая те параметры, что указаны в invoke. В данном случае один параметр - 
callback: Style.() -> Unit. Так как это лямбда, то скобочки необязательны.

Осталось научиться в react нарисовать input, а в html просто текст. Хочется получить вот такой синтаксис:
<source lang="kotlin">
react {
  search(search, onChangeSearch)
} html {
  +(search?:"")
}
</source>

Для этого нужно, чтобы react вернул объект, на котором можно было вызвать infix функцию и передать лямбду для html. 
Модификатор infix позволяет вызывать html без точки. Очень похоже на if {} else {}   
<source lang="kotlin">
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
</source>

У нас всё готово для того, чтобы нарисовать таблицу из начала статьи, но этот код уже будет формировать как reactDom, так и html.

<source lang="kotlin">
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
          search(search, onChangeSearch) //(*)
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
</source>

Обратите внимание на (*) - здесь ровно та же функция search, что и в изначальном варианте таблицы для react. Нет необходимости
переносить в новый dsl всё, что мы должны нарисовать в таблице, только общие теги.

При написании dsl получается много дополнительного кода нацеленного исключительно на форму использования. Причем используется очень много
возможностей Kotlin, которыми в повседневии не пользуешься. 
Возможно, в других случае будет иначе, но в данном случае вышло ещё и очень много дублирования от которого я так и не смог избавиться. 

Но зато у меня вышло построить dsl практически идентичный по виду react и html api (я почти не подглядывал).

