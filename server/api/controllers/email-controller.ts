import {User} from "../models/user";
import {Tracker} from "../models/tracker";
import {Email} from "../models/email";

export class EmailController {

    constructor() {}

    public async saveEmail(userId: string, trackerId: string, finderId: string) {
        // we must send 2 emails: one to the finder, and one to the one whose tracker was found
        let user = await User.findById(userId).exec() as any;
        let tracker = await Tracker.findById(trackerId).populate('lastPosition').exec() as any;
        let message = `<b>Dear ${user.firstName}</b><br/><br/>,
Your device ${tracker.name} has been found on ${tracker.lastPosition.creationDate} at the positions:<br/>
Latitude: ${tracker.lastPosition.lat}<br/>
Lonitude: ${tracker.lastPosition.lng}<br/><br/>
Go grab it as soon as you can!`;

        await new Email({user: userId, message: message}).save();

        let finder = await User.findById(finderId).exec() as any;
        message = `<b>Dear ${finder.firstName}</b><br/><br/>,
Thank you for participating in our community project, we hope our gratitude will suffice for now,<br/>
Found device: ${tracker.name}<br/>
Latitude: ${tracker.lastPosition.lat}<br/>
Lonitude: ${tracker.lastPosition.lng}<br/><br/>
You received a star for your efforts! Climb to the top of the leaderboards.`;

        await new Email({user: finderId, message: message}).save();
    }

}