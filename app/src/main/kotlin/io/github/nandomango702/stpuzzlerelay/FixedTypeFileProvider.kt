package io.github.nandomango702.stpuzzlerelay

import android.net.Uri
import androidx.core.content.FileProvider

class FixedTypeFileProvider : FileProvider() {
    override fun getType(uri: Uri): String = GamePlay.MIME_TYPE
}