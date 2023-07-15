package me.lomovtsev.hello

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.gl.ClearBufferMask

suspend fun main(args: Array<String>) {

    val app = createLittleKtApp{
        width = 1024
        height = 756
        vSync = true
        title = "My first Little Kt app"
    }
    app.start { MyGame(it) }
}

class MyGame(context: Context) : ContextListener(context) {
    override suspend fun Context.start() {
        val texture = resourcesVfs["newava.jpg"].readTexture()
        onRender {
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        }
    }
}
