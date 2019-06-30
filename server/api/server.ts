import {default as Application} from "./app";
import {Tracker} from "./models/tracker";
import Timeout = NodeJS.Timeout;
import {EmailController} from "./controllers/email-controller";
const PORT = 3000;

export class Server {

    // private app: Application;
    private readonly MAX_UPDATE_TIMEOUT = 30000;
    private readonly TIMER_TIMEOUT = 10000;

    constructor() {
        // this.app = new Application();
        this.setupApp();
        // this.startCleaner();
    }

    private setupApp(): void {
        Application.listen(PORT, () => {
            console.log('Express server listening on port ' + PORT);
        })
    }

    private startCleaner(): void {
        // disable to save power for now
        // const timer: Timeout = setInterval(this.updateLost, this.TIMER_TIMEOUT, this.MAX_UPDATE_TIMEOUT);
        const EmailTimer: Timeout = setTimeout(this.sendTestEmail, 10000);

    }

    private updateLost(timeout: number): void {
        let limit: number = Date.now() - timeout;
        console.log(Date.now());
        console.log(timeout);
        Tracker.update({lastUpdated: {$lt: limit}}, {$set: {lost: true}}, {multi: true}, (err, docs) => {
            if (err) {
                console.log(err);
            } else {
                console.log(docs);
            }
        });
    }

    private sendTestEmail() {
        console.log('Sending test email...');
        new EmailController().saveEmail('5c718f6b300e3206c82b2450', '5c79cc9c3971cc29d3d06031','5c62e3a802cf281461b308d3')
            .then(value => {
                console.log('Received email value:');
                console.log(value);
            })

    }
}

new Server();
