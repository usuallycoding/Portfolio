package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ui.PortfolioTheme
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CropPanelTest {
    private fun photo(): SourceImage {
        val image = BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(220, 120, 40); g.fillRect(0, 0, 600, 800)
        g.color = Color(40, 120, 220); g.fillRect(600, 0, 600, 800)
        g.color = Color.WHITE; g.fillOval(500, 300, 200, 200)
        g.dispose()
        return ImageProcessing.decode(ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray())!!
    }

    @Test
    fun panelShowsTheFrameAndControls() = runDesktopComposeUiTest(700, 500) {
        val source = photo()
        setContent {
            PortfolioTheme { CropPanel(source, CropAspect.Cover, CropState(source.width, source.height, CropAspect.Cover), Modifier.fillMaxSize().padding(16.dp)) }
        }
        onNodeWithText("Zoom").assertIsDisplayed()
        onNodeWithText("Reset").assertIsDisplayed()
        onNodeWithText("Drag to move, scroll or pinch to zoom. The framed 3:1 area is what gets uploaded.").assertIsDisplayed()
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "admin-crop-cover.png"))
    }

    @Test
    fun scrollingTheWheelZoomsIn() = runDesktopComposeUiTest(700, 500) {
        val source = photo()
        val state = CropState(source.width, source.height, CropAspect.Square)
        setContent { PortfolioTheme { CropPanel(source, CropAspect.Square, state, Modifier.fillMaxSize().padding(16.dp)) } }
        assertEquals(1f, state.zoom)
        onRoot().performMouseInput { moveTo(center); scroll(-2f) }
        waitForIdle()
        assertTrue(state.zoom > 1f, "zoom was ${state.zoom}")
    }

    @Test
    fun dialogConfirmReturnsTheFramedAreaAndCancelReturnsNothing() = runDesktopComposeUiTest(900, 700) {
        val source = photo()
        var confirmed: CropRect? = null
        var cancelled = false
        setContent { PortfolioTheme { CropDialog(source, CropAspect.Thumbnail, { confirmed = it }, { cancelled = true }) } }
        onNodeWithText("Crop image (16:9)").assertIsDisplayed()
        onNodeWithText("Crop & upload").performClick()
        val rect = confirmed!!
        assertEquals(1200f, rect.width)
        assertEquals(675f, rect.height)
        onNodeWithText("Cancel").performClick()
        assertTrue(cancelled)
    }
}
