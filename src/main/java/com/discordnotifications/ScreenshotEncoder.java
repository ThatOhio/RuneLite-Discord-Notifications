package com.discordnotifications;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
final class ScreenshotEncoder
{
	static final int MAX_IMAGE_BYTES = 8_000_000;
	private static final float JPEG_QUALITY = 0.85f;

	private ScreenshotEncoder()
	{
	}

	static byte[] encode(Image image)
	{
		if (!(image instanceof BufferedImage))
		{
			return null;
		}

		try
		{
			return encodeUnderLimit((BufferedImage) image);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Error converting image to byte array", e);
			return null;
		}
	}

	static byte[] encodeUnderLimit(BufferedImage image) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		byte[] png = out.toByteArray();
		if (png.length <= MAX_IMAGE_BYTES)
		{
			return png;
		}

		byte[] jpeg = encodeJpeg(scaleToRgb(image, Math.sqrt((double) MAX_IMAGE_BYTES / png.length)));
		if (jpeg.length > MAX_IMAGE_BYTES)
		{
			// Discord would reject the whole post, the text is still worth sending
			log.warn("Screenshot still {} bytes after scaling, sending without it", jpeg.length);
			return null;
		}
		return jpeg;
	}

	private static BufferedImage scaleToRgb(BufferedImage image, double factor)
	{
		int width = Math.max(1, (int) (image.getWidth() * factor));
		int height = Math.max(1, (int) (image.getHeight() * factor));

		// The JPEG writer writes nothing at all for an image with alpha
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(image, 0, 0, width, height, null);
		graphics.dispose();
		return scaled;
	}

	private static byte[] encodeJpeg(BufferedImage image) throws IOException
	{
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageOutputStream stream = ImageIO.createImageOutputStream(out))
		{
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
			writer.setOutput(stream);
			writer.write(null, new IIOImage(image, null, null), param);
			stream.flush();
			return out.toByteArray();
		}
		finally
		{
			writer.dispose();
		}
	}

	// The queue only carries bytes. PNG starts with 0x89, JPEG with FF D8
	static boolean isJpeg(byte[] image)
	{
		return image != null && image.length >= 2 && image[0] == (byte) 0xFF && image[1] == (byte) 0xD8;
	}
}
