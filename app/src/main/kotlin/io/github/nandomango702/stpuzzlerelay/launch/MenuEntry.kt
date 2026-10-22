package io.github.nandomango702.stpuzzlerelay.launch

import io.github.nandomango702.stpuzzlerelay.backend.UsedByJNI

class MenuEntry private constructor(
    val id: Int, val title: String, val submenu: Array<MenuEntry>?, val params: String?
) {

    @UsedByJNI
    constructor(id: Int, title: String, submenu: Array<MenuEntry>?) : this(id, title, submenu, null)

    @UsedByJNI
    constructor(id: Int, title: String, params: String?) : this(id, title, null, params)
}