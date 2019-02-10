import {default as Application} from "./app";
const PORT = 3000;

export class Server {

    // private app: Application;

    constructor() {
        // this.app = new Application();
        this.setupApp();
    }

    private setupApp(): void {
        Application.listen(PORT, () => {
            console.log('Express server listening on port ' + PORT);
        })
    }
}

new Server();
