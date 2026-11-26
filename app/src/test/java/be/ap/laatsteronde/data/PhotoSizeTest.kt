package be.ap.laatsteronde.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests the size math of the photos (the Bitmap work itself needs a phone). */
class PhotoSizeTest {

    @Test
    fun scaledSize_makesTheLongestSide800AndKeepsTheProportions() {
        assertEquals(800 to 600, scaledSize(4000, 3000, PHOTO_MAX_SIDE)) // landscape
        assertEquals(600 to 800, scaledSize(3000, 4000, PHOTO_MAX_SIDE)) // portrait
        assertEquals(800 to 800, scaledSize(1200, 1200, PHOTO_MAX_SIDE)) // square
    }

    @Test
    fun scaledSize_leavesASmallPhotoAsItIs() {
        assertEquals(240 to 320, scaledSize(240, 320, PHOTO_MAX_SIDE)) // a camera preview
        assertEquals(800 to 450, scaledSize(800, 450, PHOTO_MAX_SIDE))
    }

    @Test
    fun scaledSize_neverGivesZeroPixels() {
        assertEquals(800 to 1, scaledSize(10_000, 2, PHOTO_MAX_SIDE))
    }

    @Test
    fun sampleSize_readsSmallerButNeverBelow800() {
        assertEquals(1, sampleSizeFor(1000, 750, PHOTO_MAX_SIDE)) // half would be 500: too small
        assertEquals(2, sampleSizeFor(1600, 1200, PHOTO_MAX_SIDE))
        assertEquals(4, sampleSizeFor(4000, 3000, PHOTO_MAX_SIDE)) // 1000 px, then scaled to 800
        assertEquals(1, sampleSizeFor(320, 240, PHOTO_MAX_SIDE))
    }
}
