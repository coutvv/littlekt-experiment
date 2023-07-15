package me.lomovtsev.flappy.bird

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.async.newSingleThreadAsyncContext
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readAudioClip
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graph.node.resource.InputEvent
import com.lehaine.littlekt.graph.node.resource.NinePatchDrawable
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.NinePatch
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.getAnimation
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration

class FlappyBirdGame(context: Context) : ContextListener(context) {

    private var state: FbGameState = FbGameState.INIT

    private var score = 0
    private var best = 0

    override suspend fun Context.start() {
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val pixelFont = resourcesVfs["m5x7_16_outline.fnt"]
            .readBitmapFont(preloadedTextures = listOf(atlas["m5x7_16_outline_0"].slice))

        val pipeHead = atlas.getByPrefix("pipeHead").slice
        val pipeBody = atlas.getByPrefix("pipeBody").slice

        val pauseSlice = atlas.getByPrefix("pauseButton").slice
        val resumeSlice = atlas.getByPrefix("resumeButton").slice
        val startButton = atlas.getByPrefix("startButton").slice
        val panel9Slice = atlas.getByPrefix("panel_9").slice

        val audioCtx = newSingleThreadAsyncContext()
        val flapSfx = resourcesVfs["sfx/flap.wav"].readAudioClip()
        val scoreSfx = resourcesVfs["sfx/coinPickup0.wav"].readAudioClip()

        val batch = SpriteBatch(this)
        val gameViewport = ExtendViewport(135, 256)
        val gameCamera = gameViewport.camera
        val viewBounds = Rect()

        val bird = Bird(atlas.getAnimation("bird"), 12f, 10f).apply {
            y = 256 / 2f
        }

        val backgrounds = List(7) {
            val bg = atlas.getByPrefix("cityBackground").slice
            TexturedEnvironmentObject(bg, totalToWait = 2, hasCollsion = false).apply {
                x = it * bg.width.toFloat() - (bg.width * 2)
            }
        }
        val groundTiles = List(35) {
            val tileIdx = Random.nextFloat().roundToInt()
            val tile = atlas.getByPrefix("terrainTile$tileIdx").slice
            TexturedEnvironmentObject(tile, totalToWait = 10, hasCollsion = true).apply {
                x = it * tile.width.toFloat() - (tile.width * 10)
                y = 256f - tile.height
            }
        }

        val groundHeight = atlas.getByPrefix("terrainTile0").slice.height
        val totalPipesToSpawn = 10
        val pipeOffset = 100
        val pipes = List(totalPipesToSpawn) {
            Pipe(
                pipeHead = pipeHead,
                pipeBody = pipeBody,
                offsetX = pipeOffset * totalPipesToSpawn,
                availableHeight = gameViewport.virtualHeight.toInt(),
                groundOffset = groundHeight
            ).apply {
                x = pipeOffset.toFloat() + pipeOffset * it
            }

        }

        fun reset() {
            state = FbGameState.INIT
            score = 0

            gameCamera.position.x = 0f
            bird.x = 0f
            bird.y = 256 / 2f
            bird.update(Duration.ZERO)
            bird.reset()

            backgrounds.forEachIndexed { index, bg ->
                bg.apply {
                    x = index * texture.width.toFloat() - (texture.width * 2)
                }
            }

            groundTiles.forEachIndexed { index, tile ->
                tile.apply {
                    x = index * texture.width.toFloat() - (texture.width * 2)
                    y = 256f - texture.height
                }
            }

            pipes.forEachIndexed { index, pipe ->
                pipe.apply {
                    x = pipeOffset.toFloat() + pipeOffset * index
                    generate()
                }
            }
        }

        val ui = sceneGraph(this, ExtendViewport(135, 256)) {
            textureRect { // pause button
                x = 10f
                y = 10f
                slice = pauseSlice
                onUpdate += {
                    visible = state == FbGameState.STARTED
                }
                onUiInput += uiInput@{
                    if (state == FbGameState.STARTED) {
                        if (it.type == InputEvent.Type.TOUCH_DOWN) {
                            state = FbGameState.PAUSED
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
                    visible = state == FbGameState.GAME_OVER
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
                            if (state == FbGameState.GAME_OVER) {
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
                    visible = state == FbGameState.PAUSED
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
                            if (state == FbGameState.PAUSED) {
                                if (it.type == InputEvent.Type.TOUCH_DOWN) {
                                    state = FbGameState.STARTED // TODO: check for other states!!!
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
                    visible = state == FbGameState.STARTED
                }

                onUiInput += {
                    println(it.type)
                    if (it.type == InputEvent.Type.TOUCH_DOWN) {
                        state = FbGameState.STARTED // TODO: check!
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
                    visible = state == FbGameState.STARTED
                    text = "$score"
                }
            }

            textureRect {
                anchorRight = 1f
                anchorTop = 0.2f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("gameOverText").slice
                onUpdate += {
                    visible = state == FbGameState.GAME_OVER
                }
            }

            textureRect {
                anchorRight = 1f
                anchorTop = 0.2f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("getReadyText").slice
                onUpdate += {
                    visible = state == FbGameState.INIT
                }
            }
        }.also { it.initialize() }

        fun saveScore() {
            best = vfs.loadString("best")?.toInt() ?: 0
            if (score > best) {
                vfs.store("best", score.toString())
                best = score
            }
        }

        fun handleGameLogic(dt: Duration) {
            run pipeCollisionCheck@{
                pipes.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.speedMultiplier = 0f
                        state = FbGameState.GAME_OVER
                        saveScore()
                        return@pipeCollisionCheck
                    } else if (it.intersectingScore(bird.collider)) {
                        KtScope.launch(audioCtx) {
                            scoreSfx.play(0.5f)
                        }
                        it.collect()
                        score++
                    }
                }
            }

            run groundCollisionCheck@{
                groundTiles.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.die()
                        state = FbGameState.GAME_OVER
                        saveScore()
                        return@groundCollisionCheck
                    }
                }
            }

            bird.update(dt)
            if (bird.y < 0) {
                bird.y = 0f
            }

            if (state == FbGameState.STARTED) {
                if (input.isJustTouched(Pointer.POINTER1) || input.isKeyJustPressed(Key.SPACE)) {
                    bird.flap()
                    KtScope.launch(audioCtx) {
                        flapSfx.play()
                    }
                }
            } else if (state == FbGameState.GAME_OVER) {
                if (input.isKeyJustPressed(Key.SPACE)) {
                    reset()
                }
            } else if (state == FbGameState.INIT) {
                if (input.isKeyJustPressed(Key.SPACE)) {
                    state = FbGameState.STARTED
                }
            }
        }

        fun handleStartMenu() {
            if (input.isJustTouched(Pointer.POINTER1) || input.isKeyJustPressed(Key.SPACE)) {
                state = FbGameState.STARTED
            }
        }


        onResize { widht, height ->
            println("${graphics.width}, ${graphics.height}")
            gameViewport.update(widht, height, context, true)
            ui.resize(widht, height, true)
        }
        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (state == FbGameState.STARTED) {
                handleGameLogic(dt)
            } else {
                handleStartMenu()
            }

            gameCamera.position.x = bird.x.roundToInt() + 20f
            gameViewport.apply(this)
            gameCamera.update()
            viewBounds.calculateViewBounds(gameCamera)

            backgrounds.forEach {
                it.update(viewBounds)
            }
            groundTiles.forEach {
                it.update(viewBounds)
            }
            pipes.forEach {
                it.update(viewBounds)
            }

            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            batch.use(gameCamera.viewProjection) { batch ->
                backgrounds.forEach { it.render(batch) }
                groundTiles.forEach { it.render(batch) }
                pipes.forEach { it.render(batch) }
                bird.render(batch)
            }

            ui.update(dt)
            ui.render()
        }
        onPostRender {
            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }
            if (input.isKeyJustPressed(Key.R)) {
                reset()
            }
        }
        onDispose {
            atlas.dispose()
        }
    }
}

/**
 * INIT      -----> STARTED
 *
 * STARTED   -----> GAME_OVER
 *           \----> PAUSED
 *
 * PAUSED    -----> STARTED
 *
 * GAME_OVER -----> INIT
 */
enum class FbGameState {
    STARTED, PAUSED, GAME_OVER, INIT
}