import {Application} from "express";
import {UserController} from '../controllers/user-controller';

export class UserRoutes {

    private controller: UserController;

    constructor() {
        this.controller = new UserController();
    }

    public applyRoutes(app: Application): void {
        app.route('/resource/user')
            .get(this.controller.getAll);

        app.route('/user/:userId')
            .get(this.controller.getUserMode);

        app.route('/user')
            .post(this.controller.saveUser);
    }
}