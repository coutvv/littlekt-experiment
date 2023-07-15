package me.lomovtsev.flappy.bird

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graph.node.resource.InputEvent
import com.lehaine.littlekt.graph.node.resource.NinePatchDrawable
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.g2d.NinePatch
import com.lehaine.littlekt.util.viewport.ExtendViewport
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

data class Ui (
    private val context: Context,
    private val state: AtomicReference<FbGameState>,
    val score: AtomicInteger,
    val best: AtomicInteger
){
    suspend fun makeUi(context: Context, reset: () -> Unit): SceneGraph<String> {

        val resourcesVfs = context.resourcesVfs

        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val pixelFont = resourcesVfs["m5x7_16_outline.fnt"]
            .readBitmapFont(preloadedTextures = listOf(atlas["m5x7_16_outline_0"].slice))

        val pauseSlice = atlas.getByPrefix("pauseButton").slice
        val resumeSlice = atlas.getByPrefix("resumeButton").slice
        val startButton = atlas.getByPrefix("startButton").slice
        val panel9Slice = atlas.getByPrefix("panel_9").slice

        val ui: SceneGraph<String> = sceneGraph(context, ExtendViewport(135, 256)) {
            textureRect { // pause button
                x = 10f
                y = 10f
                slice = pauseSlice
                onUpdate += {
                    visible = state.get() == FbGameState.STARTED
                }
                onUiInput += uiInput@{
                    if (state.get() == FbGameState.STARTED) {
                        if (it.type == InputEvent.Type.TOUCH_DOWN) {
                            state.set(FbGameState.PAUSED)
                            it.handle()
                        }
                    }
                }
            }

            // score
            panel {
                name = "Score Container"
                panel = NinePatchDrawable(NinePatch(panel9Slice, 2, 2, 2, 4))

                anchorBottom = 0.8f
                anchorTop = 0.3f
                anchorLeft = 0.1f
                anchorRight = 0.9f

                onUpdate += {
                    visible = state.get() == FbGameState.GAME_OVER
                }

                vBoxContainer {
                    separation = 10
                    marginTop = 5f
                    anchorRight = 1f
                    anchorBottom = 1f

                    label {
                        font = pixelFont
                        horizontalAlign = HAlign.CENTER

                        onUpdate += {
                            text = "Score: $score"
                        }
                    }

                    label {
                        font = pixelFont
                        horizontalAlign = HAlign.CENTER

                        onUpdate += {
                            text = "Best Score: $best"
                        }
                    }

                    textureRect {
                        anchorTop = 1f
                        anchorBottom = 1f
                        anchorRight = 1f

                        marginTop = -50f
                        slice = startButton
                        stretchMode = TextureRect.StretchMode.KEEP_CENTERED

                        onUiInput += {
                            if (state.get() == FbGameState.GAME_OVER) {
                                if (it.type == InputEvent.Type.TOUCH_DOWN) {
                                    reset()
                                }
                            }
                        }
                    }
                }
            }

            centerContainer {
                anchorRight = 1f
                anchorBottom = 1f
                onUpdate += {
                    visible = state.get() == FbGameState.PAUSED
                }

                vBoxContainer {
                    separation = 10

                    label {
                        text = "tap to Resume"
                        font = pixelFont
                        horizontalAlign = HAlign.CENTER
                    }

                    textureRect {
                        slice = resumeSlice
                        stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                        onUiInput += uiInput@{
                            if (state.get() == FbGameState.PAUSED) {
                                if (it.type == InputEvent.Type.TOUCH_DOWN) {
                                    state.set(FbGameState.STARTED) // TODO: check for other states!!!
                                    it.handle()
                                }
                            }
                        }
                    }
                }
            }

            textureRect {
                x = 10f
                y = 10f
                slice = pauseSlice

                onUpdate += {
                    visible = state.get() == FbGameState.STARTED
                }

                onUiInput += {
                    println(it.type)
                    if (it.type == InputEvent.Type.TOUCH_DOWN) {
                        state.set(FbGameState.STARTED) // TODO: check!
                        it.handle()
                    }
                }
            }

            label {
                anchorRight = 1f
                anchorTop = 0.1f
                text = "0"
                font = pixelFont
                horizontalAlign = HAlign.CENTER

                onUpdate += {
                    visible = state.get() == FbGameState.STARTED
                    text = "$score"
                }
            }

            textureRect {
                anchorRight = 1f
                anchorTop = 0.2f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("gameOverText").slice
                onUpdate += {
                    visible = state.get() == FbGameState.GAME_OVER
                }
            }

            textureRect {
                anchorRight = 1f
                anchorTop = 0.2f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("getReadyText").slice
                onUpdate += {
                    visible = state.get() == FbGameState.INIT
                }
            }
        }.also { it.initialize() }
        return ui
    }
}