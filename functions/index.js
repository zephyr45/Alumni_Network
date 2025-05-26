const functions = require("firebase-functions");
const admin = require("firebase-admin");
const sgMail = require("@sendgrid/mail");
require("dotenv").config();


admin.initializeApp();
sgMail.setApiKey(process.env.SENDGRID_API_KEY);


// Define a function to send email
exports.sendEmail = functions.https.onCall((data, context) => {
  const msg = {
    to: data.email, // recipient email
    from: "gouravagarwal014@gmail.com",
    subject: data.subject,
    text: data.message,
  };

  return sgMail
      .send(msg)
      .then(() => {
        return {success: true};
      })
      .catch((error) => {
        console.error("Error sending email:", error);
        return {success: false, error: error.message};
      });
});
