package com.example.lens.controller;

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
public class HelloController {

    // method to return index.html as model and view
    @GetMapping("/index")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("index");
        return modelAndView;
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

        for (Rect face : faceDetections.toArray()) {
            // Draw a rectangle (the frame) around the face
            Imgproc.rectangle(imageMat, face.tl(), face.br(), new Scalar(0, 255, 0), 2);


            // Detect eyes within the face
            MatOfRect eyeDetections = new MatOfRect();
            eyeDetector.detectMultiScale(imageMat, eyeDetections);

            for (Rect eye : eyeDetections.toArray()) {
                // Draw a rectangle (the frame) around the eyes
                Imgproc.rectangle(imageMat, eye.tl(), eye.br(), new Scalar(255, 0, 0), 2);
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
