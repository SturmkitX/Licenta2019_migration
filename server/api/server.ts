import {default as Application} from "./app";
import {Tracker} from "./models/tracker";
import Timeout = NodeJS.Timeout;
const PORT = 3000;

export class Server {

    // private app: Application;
    private readonly MAX_UPDATE_TIMEOUT = 30000;
    private readonly TIMER_TIMEOUT = 10000;

    constructor() {
        // this.app = new Application();
        this.setupApp();
        this.startCleaner();
    }

    private setupApp(): void {
        Application.listen(PORT, () => {
            console.log('Express server listening on port ' + PORT);
        })
    }

    private startCleaner(): void {
        // disable to save power for now
        // const timer: Timeout = setInterval(this.updateLost, this.TIMER_TIMEOUT, this.MAX_UPDATE_TIMEOUT);

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
}

new Server();
