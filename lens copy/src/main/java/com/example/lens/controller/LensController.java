package com.example.lens.controller;

import com.example.lens.model.FrameRequest;
import com.example.lens.model.ImageData;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Base64;

@Controller
@RequestMapping("/")
public class LensController {

    private String frame = "black.webp";

    // method to return index.html as model and view
    @GetMapping("/")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("home");
        return modelAndView;
    }

    @GetMapping("/about")
    public ModelAndView getAbout() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("about");
        return modelAndView;
    }


    @GetMapping("/tryFrames")
    public ModelAndView tryFrames() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("newindex");
        return modelAndView;
    }

    @GetMapping("/contact")
    public ModelAndView getContact() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("contact");
        return modelAndView;
    }



    @PostMapping("/frame")
    public ResponseEntity<String> receiveFrame(@RequestBody FrameRequest frame ) {
        System.out.println("Frame received: " + frame);
        this.frame = frame.getFrameName();
        System.out.println("Frame changed");
        return new ResponseEntity<>("Frame changed", HttpStatus.OK);
    }

    @PostMapping("/image")
    public ResponseEntity<String> receiveImageData(@RequestBody ImageData imageData) {

        String base64ImageData = imageData.getImageData().split(",")[1];
        byte[] decodedImageData = Base64.getDecoder().decode(base64ImageData);
        MatOfByte matOfByte = new MatOfByte(decodedImageData);
        Mat imageMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);

        // Load the cascade classifier
        CascadeClassifier faceDetector = new CascadeClassifier("resources/openCVResources/haarcascade_frontalface_alt.xml");
        CascadeClassifier eyeDetector = new CascadeClassifier("resources/openCVResources/haarcascade_eye.xml");

        // Detect faces in the image
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(imageMat, faceDetections);

        // Load the eyeglass image
        Mat eyeglassMat = Imgcodecs.imread("resources/images/" + frame, Imgcodecs.IMREAD_UNCHANGED);

        // Check if the eyeglass image has been properly loaded
        if (eyeglassMat.empty()) {
            System.out.println("Failed to load eyeglass image");
            return new ResponseEntity<>("Failed to load eyeglass image", HttpStatus.INTERNAL_SERVER_ERROR);
        }


        for (Rect face : faceDetections.toArray()) {

            // Detect eyes within the face
            MatOfRect eyeDetections = new MatOfRect();
            eyeDetector.detectMultiScale(imageMat, eyeDetections);

            Rect eye = eyeDetections.toArray()[0];
            for (Rect foundEyes : eyeDetections.toArray()) {
                if (foundEyes.y > face.y) {
                    eye = foundEyes;
                    break;
                }
            }


            // Define the percentage of the face width you want to use
            double percentage = 0.3;

            // Calculate the new width and height as a percentage of the face width
            double newWidth = face.width;
            double newHeight = face.width * percentage;

            // Resize the eyeglass image to match the new width and height
            Mat resizedEyeglassMat = new Mat();
            Size size = new Size(newWidth, newHeight);
            Imgproc.resize(eyeglassMat, resizedEyeglassMat, size);

            // Define the region of interest (ROI) to match the dimensions of the resized eyeglass image
            Rect roiRect = new Rect(face.x, eye.y, resizedEyeglassMat.cols(), resizedEyeglassMat.rows());
            Mat roi = imageMat.submat(roiRect);
            Mat roiCopy = imageMat.submat(roiRect);


            // Convert roi and resizedEyeglassMat to 4-channel images if they are not
            if (roi.channels() == 3) {
                Imgproc.cvtColor(roi, roi, Imgproc.COLOR_BGR2BGRA);
            }

            for (int i = 0; i < resizedEyeglassMat.rows(); i++) {
                for (int j = 0; j < resizedEyeglassMat.cols(); j++) {
                    double[] data = resizedEyeglassMat.get(i, j);
                    double[] roiData = roi.get(i, j);
                    if (data[3] == 0) {
                        data = roiData;
                        resizedEyeglassMat.put(i, j, data);
                    }
                }
            }


            // Check if the ROI and the eyeglass image have the same size
            if (roi.size().equals(resizedEyeglassMat.size())) {
                // Superimpose the resized eyeglass image on the original image at the location of the ROI
                Core.addWeighted(roi, 0.5, resizedEyeglassMat, 0.5, 0, roi);
                Imgproc.cvtColor(roi, roi, Imgproc.COLOR_BGRA2BGR);
                Core.addWeighted(roiCopy, 0, roi, 1, 0, roiCopy);
            } else {
                System.out.println("The ROI and the eyeglass image do not have the same size");
            }

        }

        // Convert the Mat object to a byte array
        MatOfByte convertedMat = new MatOfByte();
        Imgcodecs.imencode(".jpg", imageMat, convertedMat);
        byte[] imageBytes = convertedMat.toArray();

        // Encode the byte array to a base64 string
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Create the data URI
        String dataURI = "data:image/jpeg;base64," + base64Image;

        // Return the data URI
        return new ResponseEntity<>(dataURI, HttpStatus.OK);
    }

}
