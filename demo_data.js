// Add this data to Firebase through the Firebase Console

// 1. Create a network document in the "networks" collection
// networks/{networkId}
{
  "title": "BMSCE Alumni Network",
  "address": "Bull Temple Road, Bangalore",
  "image": "https://upload.wikimedia.org/wikipedia/en/thumb/8/87/BMS_College_of_Engineering.svg/1200px-BMS_College_of_Engineering.svg.png",
  "description": "Official alumni network for BMSCE graduates"
}

// 2. Add some internship posts
// networks/{networkId}/internships/{internshipId}
{
  "senderId": "{YOUR_USER_ID}",
  "senderName": "John Doe",
  "timeStamp": "Timestamp",
  "companyName": "Google",
  "duration": "6 months",
  "jobTitle": "Software Engineering Intern",
  "location": "Bangalore",
  "stipend": "50000",
  "applyLink": "https://careers.google.com",
  "description": "Looking for talented software engineering interns to join our team.",
  "likes": [],
  "noOfLikes": 0
}

// 3. Add some job posts
// networks/{networkId}/jobs/{jobId}
{
  "senderId": "{YOUR_USER_ID}",
  "senderName": "Jane Smith",
  "timeStamp": "Timestamp",
  "companyName": "Microsoft",
  "minimumExperience": "2 years",
  "jobTitle": "Senior Software Engineer",
  "location": "Bangalore",
  "salary": "25 LPA",
  "applyLink": "https://careers.microsoft.com",
  "description": "Looking for experienced software engineers to join our cloud team.",
  "likes": [],
  "noOfLikes": 0
}

// 4. Add some discussions
// networks/{networkId}/discussions/{discussionId}
{
  "senderId": "{YOUR_USER_ID}",
  "name": "Alex Johnson",
  "timestamp": "Timestamp",
  "description": "What are the best resources for learning cloud computing?",
  "likes": [],
  "comments": [],
  "noOfLikes": 0,
  "imageUrl": "",
  "category": "Technology"
}

// 5. Add network users
// networks/{networkId}/networkusers/{userId}
{
  "joinedAt": "Timestamp"
}

// 6. Update user profile
// users/{userId}
{
  "FirstName": "Your",
  "LastName": "Name",
  "PhoneNo": "+1234567890",
  "currentWorking": "Student/Professional",
  "JobTitle": "Software Engineer",
  "CompanyName": "Tech Company",
  "city": "Bangalore",
  "industry": "Software",
  "imageUrl": "https://ui-avatars.com/api/?name=Your+Name",
  "networks": ["{networkId}"]
} 