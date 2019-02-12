import {Application} from "express";
import {UserController} from '../controllers/user-controller';
import * as Perm from 'express-jwt-permissions';

export class UserRoutes {

    private controller: UserController;
    private guard: any;

    constructor() {
        this.controller = new UserController();
        this.guard = new Perm();
    }

    public applyRoutes(app: Application): void {
        app.route('/resource/user')
            .get(this.guard.check('ADMIN'), this.controller.getAll);

        app.route('/resource/user/:userId')
            .get(this.guard.check('ADMIN'), this.controller.getSpecificUser);

        app.route('/resource/user/me')
            .get(this.controller.getSelf);

        app.route('/resource/user')
            .post(this.guard.check('ADMIN'), this.controller.saveUser);
    }
}