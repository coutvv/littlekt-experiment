package me.lomovtsev.flappy.bird

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.async.newSingleThreadAsyncContext
import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readAudioClip
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.TextureAtlas
import com.lehaine.littlekt.graphics.g2d.getAnimation
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration

class FlappyBirdGame(context: Context) : ContextListener(context) {

    private var state: AtomicReference<FbGameState> = AtomicReference(FbGameState.INIT)

    private var score = AtomicInteger(0)
    private var best = AtomicInteger(0)

    lateinit var bird: Bird
    lateinit var gameCamera: Camera
    lateinit var groundTiles: List<TexturedEnvironmentObject>
    lateinit var backgrounds: List<TexturedEnvironmentObject>
    lateinit var pipes: List<Pipe>

    private fun generatePipes(atlas: TextureAtlas, gameViewport: ExtendViewport): List<Pipe> {
        val pipeHead = atlas.getByPrefix("pipeHead").slice
        val pipeBody = atlas.getByPrefix("pipeBody").slice
        val groundHeight = atlas.getByPrefix("terrainTile0").slice.height
        val totalPipesToSpawn = 10
        val pipes = List(totalPipesToSpawn) {
            Pipe(
                pipeHead = pipeHead,
                pipeBody = pipeBody,
                offsetX = PIPE_OFFSET * totalPipesToSpawn,
                availableHeight = gameViewport.virtualHeight.toInt(),
                groundOffset = groundHeight
            ).apply {
                x = PIPE_OFFSET.toFloat() + PIPE_OFFSET * it
            }

        }
        return pipes
    }

    private fun generateBackgrounds(atlas: TextureAtlas): List<TexturedEnvironmentObject> {
        val backgrounds = List(7) {
            val bg = atlas.getByPrefix("cityBackground").slice
            TexturedEnvironmentObject(bg, totalToWait = 2, hasCollsion = false).apply {
                x = it * bg.width.toFloat() - (bg.width * 2)
            }
        }
        return backgrounds
    }

    private fun generateGroundTiles(atlas: TextureAtlas): List<TexturedEnvironmentObject> {
        val groundTiles = List(35) {
            val tileIdx = Random.nextFloat().roundToInt()
            val tile = atlas.getByPrefix("terrainTile$tileIdx").slice
            TexturedEnvironmentObject(tile, totalToWait = 10, hasCollsion = true).apply {
                x = it * tile.width.toFloat() - (tile.width * 10)
                y = 256f - tile.height
            }
        }
        return groundTiles
    }

    fun reset() {
        state.set(FbGameState.INIT)
        score.set(0)

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
                x = PIPE_OFFSET.toFloat() + PIPE_OFFSET * index
                generate()
            }
        }
    }

    fun saveScore(vfs: Vfs) {
        best.set(vfs.loadString("best")?.toInt() ?: 0)
        if (score.get() > best.get()) {
            vfs.store("best", score.toString())
            best = score
        }
    }

    override suspend fun Context.start() {
        val atlas: TextureAtlas = resourcesVfs["tiles.atlas.json"].readAtlas()

        val audioCtx = newSingleThreadAsyncContext()
        val flapSfx = resourcesVfs["sfx/flap.wav"].readAudioClip()
        val scoreSfx = resourcesVfs["sfx/coinPickup0.wav"].readAudioClip()

        val batch = SpriteBatch(this)
        val gameViewport = ExtendViewport(135, 256)
        gameCamera = gameViewport.camera
        val viewBounds = Rect()
        pipes = generatePipes(atlas, gameViewport)

        bird = Bird(atlas.getAnimation("bird"), 12f, 10f).apply {
            y = 256 / 2f
        }
        groundTiles = generateGroundTiles(atlas)
        backgrounds = generateBackgrounds(atlas)

        val ui = Ui(context, state, score, best).makeUi(this) {
            reset()
        }

        fun handleGameLogic(dt: Duration) {
            run pipeCollisionCheck@{
                pipes.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.speedMultiplier = 0f
                        state.set( FbGameState.GAME_OVER)
                        saveScore(vfs)
                        return@pipeCollisionCheck
                    } else if (it.intersectingScore(bird.collider)) {
                        KtScope.launch(audioCtx) {
                            scoreSfx.play(0.5f)
                        }
                        it.collect()
                        score.incrementAndGet()
                    }
                }
            }

            run groundCollisionCheck@{
                groundTiles.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.die()
                        state.set( FbGameState.GAME_OVER)
                        saveScore(vfs)
                        return@groundCollisionCheck
                    }
                }
            }

            bird.update(dt)
            if (bird.y < 0) {
                bird.y = 0f
            }

            if (state.get() == FbGameState.STARTED) {
                if (input.isJustTouched(Pointer.POINTER1) || input.isKeyJustPressed(Key.SPACE)) {
                    bird.flap()
                    KtScope.launch(audioCtx) {
                        flapSfx.play()
                    }
                }
            } else if (state.get() == FbGameState.GAME_OVER) {
                if (input.isKeyJustPressed(Key.SPACE)) {
                    reset()
                }
            } else if (state.get() == FbGameState.INIT) {
                if (input.isKeyJustPressed(Key.SPACE)) {
                    state.set(FbGameState.STARTED)
                }
            }
        }

        fun handleStartMenu() {
            if (input.isJustTouched(Pointer.POINTER1) || input.isKeyJustPressed(Key.SPACE)) {
                state.set(FbGameState.STARTED)
            }
        }


        onResize { width, height ->
            println("${graphics.width}, ${graphics.height}")
            gameViewport.update(width, height, context, true)
            ui.resize(width, height, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (state.get() == FbGameState.STARTED) {
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

    companion object {
        private const val PIPE_OFFSET = 100
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