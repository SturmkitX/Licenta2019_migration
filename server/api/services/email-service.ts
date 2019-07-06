import {User} from "../models/user";
import {Tracker} from "../models/tracker";
import {Email} from "../models/email";
import * as Mailgun from "mailgun-js";

export class EmailService {

    private readonly DOMAIN = 'sandboxef7f165352204607b789f5153c27e7cb.mailgun.org';
    private readonly API_KEY = '3b53920715e0eb6b3a6351cecc391dc5-2b778fc3-e66f126a';
    private mailService: Mailgun.Mailgun;

    constructor() {
        this.mailService = new Mailgun({apiKey: this.API_KEY, domain: this.DOMAIN});
    }

    public async saveEmail(userId: string, trackerId: string, finderId: string) {
        // we must send 2 emails: one to the finder, and one to the one whose tracker was found
        let user = await User.findById(userId).exec() as any;
        let tracker = await Tracker.findById(trackerId).populate('lastPosition').exec() as any;
        let message = `<b>Dear ${user.firstName}</b><br/><br/>,
Your device ${tracker.name} has been found on ${tracker.lastPosition.creationDate} at the positions:<br/>
Latitude: ${tracker.lastPosition.lat}<br/>
Lonitude: ${tracker.lastPosition.lng}<br/><br/>
Go grab it as soon as you can!`;

        new Email({user: userId, message: message}).save();

        let finder = await User.findById(finderId).exec() as any;
        message = `<b>Dear ${finder.firstName}</b><br/><br/>,
Thank you for participating in our community project, we hope our gratitude will suffice for now,<br/>
Found device: ${tracker.name}<br/>
Latitude: ${tracker.lastPosition.lat}<br/>
Lonitude: ${tracker.lastPosition.lng}<br/><br/>
You received a star for your efforts! Climb to the top of the leaderboards.`;

        new Email({user: finderId, message: message}).save();
    }

    public sendEmail() {
        let emailData = {
            from: 'Tracker Administration <admin@samples.mailgun.org>',
            subject: 'Tracker notification',
            to: '',
            message: ''
        };

        Email.find().populate('user').exec((err, emails) => {
            console.log('Sending emails...');
            if (err) {
                console.log(err);
                return;
            } else {
                for (let email of emails) {
                    // @ts-ignore
                    emailData.to = email.user.email;

                    // @ts-ignore
                    emailData.message = email.message;
                    this.mailService.messages().send(emailData, (err, body) => {
                        console.log(body);
                    });
                }
                Email.deleteMany({},err => {
                    // console.log('Error deleting emails', err);
                });
            }

        });
    }

}