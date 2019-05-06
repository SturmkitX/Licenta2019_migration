import {Application} from "express";
import {LogInController} from "../controllers/login-controller";
import {UserController} from "../controllers/user-controller";

export class LogInRoutes {

    private controller: LogInController;
    private userController: UserController;

    constructor() {
        this.controller = new LogInController();
        this.userController = new UserController();
    }

    public applyRoutes(app: Application): void {
        app.route('/public/login')
            .post(this.controller.login);
        app.route('/public/logout')
            .get(this.controller.logout);

        app.route('/public/register')
            .post(this.userController.saveUser);
    }
}