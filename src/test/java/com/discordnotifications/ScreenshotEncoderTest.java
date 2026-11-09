package com.discordnotifications;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScreenshotEncoderTest
{
	@Test
	public void smallScreenshotStaysPng() throws IOException
	{
		byte[] encoded = ScreenshotEncoder.encodeUnderLimit(new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB));

		assertFalse(ScreenshotEncoder.isJpeg(encoded));
		assertEquals(100, ImageIO.read(new ByteArrayInputStream(encoded)).getWidth());
	}

	@Test
	public void oversizedScreenshotBecomesScaledJpeg() throws IOException
	{
		// Noise doesn't compress, 2000x2000 is about 12 MB as PNG
		BufferedImage image = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
		Random random = new Random(1);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				image.setRGB(x, y, 0xFF000000 | random.nextInt(0x1000000));
			}
		}

		byte[] encoded = ScreenshotEncoder.encodeUnderLimit(image);

		assertTrue(ScreenshotEncoder.isJpeg(encoded));
		assertTrue(encoded.length <= ScreenshotEncoder.MAX_IMAGE_BYTES);
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(encoded));
		assertTrue(decoded.getWidth() < 2000);
		assertEquals(decoded.getWidth(), decoded.getHeight());
	}

	@Test
	public void missingImageIsNotJpeg()
	{
		assertFalse(ScreenshotEncoder.isJpeg(null));
		assertFalse(ScreenshotEncoder.isJpeg(new byte[0]));
	}
}
