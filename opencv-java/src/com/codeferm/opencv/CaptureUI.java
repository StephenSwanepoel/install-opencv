/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 *
 * Created by Steven P. Goldsmith on December 21, 2013
 * sgoldsmith@codeferm.com
 */
package com.codeferm.opencv;

import java.applet.Applet;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * A simple video capture applet. The Java bindings do not have an imshow
 * equivalent (highgui wrapper) yet.
 *
 * args[0] = camera index, url or will default to "-1" if no args passed.
 *
 * @author sgoldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
// This is demo code, not worried about magic numbers, etc.
@SuppressWarnings({ "checkstyle:magicnumber", "PMD.LawOfDemeter", "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidUsingNativeCode", "PMD.AvoidFinalLocalVariable",
        "PMD.CommentSize", "PMD.AvoidPrintStackTrace", "PMD.UseProperClassLoader", "PMD.AvoidPrefixingMethodParameters",
        "PMD.DataflowAnomalyAnalysis" })
final class CaptureUI extends Applet implements Runnable { // NOPMD
    /**
     * Serializable class version number.
     */
    private static final long serialVersionUID = -3988850198352906349L;
    /**
     * Logger.
     */
    // Logger is not a constant
    @SuppressWarnings({ "checkstyle:constantname", "PMD.VariableNamingConventions" })
    private static final Logger logger = Logger.getLogger(CaptureUI.class // NOPMD
            .getName());
    /**
     * Mat for image capture.
     */
    private transient Mat captureMat;
    /**
     * Class for video capturing from video files or cameras.
     */
    private transient VideoCapture videoCapture;
    /**
     * Frame size.
     */
    private final transient Size frameSize;
    /**
     * Applet drawing canvas.
     */
    private transient BufferedImage bufferedImage;
    /**
     * Processing thread.
     */
    @SuppressWarnings("PMD.DoNotUseThreads")
    private transient Thread captureThread;

    /* Load the OpenCV system library */
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // NOPMD
    }

    /**
     * Initialize VideoCapture.
     *
     * @param url
     *            Camera URL.
     */
    CaptureUI(final String url) {
        super();
        // See if URL is an integer: -? = negative sign, could have none or one,
        // \\d+ = one or more digits
        if (url.matches("-?\\d+")) {
            videoCapture = new VideoCapture(Integer.parseInt(url));
        } else {
            videoCapture = new VideoCapture(url);
        }
        // Custom logging properties via class loader
        try {
            LogManager.getLogManager()
                    .readConfiguration(CaptureUI.class.getClassLoader().getResourceAsStream("logging.properties"));
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        frameSize = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        logger.log(Level.INFO, String.format("OpenCV %s", Core.VERSION));
        logger.log(Level.INFO, "Press [Esc] to exit");
        logger.log(Level.INFO, String.format("URL: %s", url));
        init();
    }

    /**
     * Return capture Mat.
     *
     * @return Current Mat image from capture.
     */
    public Mat getCaptureMat() {
        return captureMat;
    }

    /**
     * VideoCapture accessor.
     *
     * @return VideoCapture.
     */
    public VideoCapture getCap() {
        return videoCapture;
    }

    /**
     * Frame size accessor.
     *
     * @return Frame size.
     */
    public Size getFrameSize() {
        return frameSize;
    }

    /**
     * Displays diagnostic information.
     */
    @Override
    public void init() {
        logger.log(Level.INFO, String.format("Resolution: %s", frameSize));
        captureMat = new Mat();
    }

    /**
     * Create frame acquisition thread and start.
     */
    @SuppressWarnings("PMD.DoNotUseThreads")
    @Override
    public void start() {
        if (captureThread == null) {
            captureThread = new Thread(this);
            captureThread.start();
        }
    }

    /**
     * Stop frame acquisition thread.
     */
    @SuppressWarnings("PMD.NullAssignment")
    @Override
    public void stop() {
        if (captureThread != null) {
            try {
                captureThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            captureThread = null;
        }
        // Release native memory
        videoCapture.free();
        if (captureMat != null) {
            captureMat.free();
        }
    }

    /**
     * Read frame, convert from Mat to byte array, repaint canvas with new
     * frame.
     */
    @Override
    public void run() {
        while (true) {
            if (videoCapture.read(captureMat)) {
                /*
                 * Add image processing code here.
                 */
                convert(captureMat);
                repaint();
            } else {
                break;
            }
        }
    }

    /**
     * Convert from Mat to BufferedImage.
     *
     * @param mat
     *            Mat array.
     */
    public void convert(final Mat mat) {
        final byte[] sourcePixels = new byte[mat.width() * mat.height() * mat.channels()];
        mat.get(0, 0, sourcePixels);
        // Create new image and get reference to backing data
        bufferedImage = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        // Fast copy
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
    }

    /**
     * Update image.
     *
     * @see java.awt.Container#update(java.awt.Graphics)
     */
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    @Override
    public synchronized void update(final Graphics graphics) {
        graphics.drawImage(bufferedImage, 0, 0, this);
    }

    /**
     * Create window, frame and set window to visible.
     *
     * args[0] = camera index, url or will default to "0" if no args passed.
     *
     * @param args
     *            String array of arguments.
     */
    public static void main(final String... args) {
        String url = null;
        // Check how many arguments were passed in
        if (args.length == 0) {
            // If no arguments were passed then default to camera index -1
            url = "-1";
        } else {
            url = args[0];
        }
        final CaptureUI window = new CaptureUI(url);
        // Deal with VideoCapture always returning True otherwise it will hang
        // on VideoCapture.read()
        if (window.frameSize.width > 0 && window.frameSize.height > 0) {
            final KeyEventFrame frame = new KeyEventFrame();
            frame.addWindowListener(new WindowAdapter() {
                /**
                 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
                 */
                @SuppressWarnings("PMD.DoNotCallSystemExit")
                @Override
                public void windowClosing(final WindowEvent event) {
                    System.exit(0);
                }
            });
            frame.add(window);
            // Set frame size based on image size
            frame.setSize((int) window.getFrameSize().width, (int) window.getFrameSize().height);
            frame.setVisible(true);
            window.start();
        } else {
            logger.log(Level.SEVERE, "Unable to open device");
        }
    }
}
