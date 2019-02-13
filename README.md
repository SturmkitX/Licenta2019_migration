# README for Final Project

# How to setup MongoDB
1. docker pull mongo
2. docker run -p 27017:27017 -d mongo

3. (to see processes) docker ps [-a]

# TODO list
# Server
-[X] MongoDB support
-[X] JWT + Permissions integration
-[ ] HTTPS (https://itnext.io/building-restful-web-apis-with-node-js-express-mongodb-and-typescript-part-5-a80e5a7f03db)
-[ ] Email confirmation
-[ ] Periodic job to process emails
-[ ] Periodic job to process account deletions

## CRUD Operations will be added as needed
### Admin
-[X] User Operations (get all, get specific, insert)
-[X] Tracker Operations (get all, get specific device, insert)
-[X] History Operations (get all, get for specific device)

### Regular user (Client + Tracker)
-[ ] User Operations (get own info, register, update own info, mark account for deletion)
-[ ] Tracker Operations (get own tracker info, pair tracker, unpair tracker)
-[ ] History Operations (get own history for a certain interval, insert)

# Arduino
-[ ] Circuit
-[ ] JSON Feature
-[ ] ESP8266 communication
-[ ] Code

# Android
-[ ] UI
-[ ] Special messages when another user finds your LOST device (optional +1 rating system, as a RATING field in user, starting at 0)