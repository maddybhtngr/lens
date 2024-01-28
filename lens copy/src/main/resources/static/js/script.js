const video = document.getElementById('webcam');
const canvas = document.getElementById('snapshot');
const context = canvas.getContext('2d');

if (navigator.mediaDevices.getUserMedia) {
    navigator.mediaDevices.getUserMedia({video: true})
        .then(function (stream) {
            video.srcObject = stream;
            setInterval(takeSnapshot, 100); // Take a snapshot every 100ms
        })
        .catch(function (error) {
            console.log("Something went wrong!");
        });
}

function takeSnapshot() {
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, video.videoWidth, video.videoHeight); // Draw the current video frame on the canvas
    const imageData = canvas.toDataURL('image/jpeg'); // Convert the canvas image to Data URL

    // Send the image data to the server
    fetch('/image', { // Change the endpoint to match the one in your Spring Boot application
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({imageData: imageData})
    })
        .then(response => response.text()) // Expect a text response
        .then(data => {
            console.log(data);
            document.getElementById('output').src = data; // Set the src attribute of the img element to the response data URI
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}