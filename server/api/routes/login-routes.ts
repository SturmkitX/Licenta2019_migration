import {Application} from "express";
import {LogInController} from "../controllers/login-controller";

export class LogInRoutes {

    private controller: LogInController;

    constructor() {
        this.controller = new LogInController();
    }

    public applyRoutes(app: Application): void {
        app.route('/public/login')
            .post(this.controller.login);
        app.route('/public/logout')
            .get(this.controller.logout);
    }
}