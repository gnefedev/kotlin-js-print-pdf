@file:JsModule("file-saver")

package com.gnefedev.react

import org.w3c.files.Blob

external fun saveAs(
    data: Blob,
    filename: String? = definedExternally /* null */,
    disableAutoBOM: Boolean? = definedExternally /* null */
): Unit = definedExternally


