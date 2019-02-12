import {Application} from "express";
import {TrackerController} from '../controllers/tracker-controller';
import * as Perm from 'express-jwt-permissions';

export class TrackerRoutes {

    private controller: TrackerController;
    private guard: any;

    constructor() {
        this.controller = new TrackerController();
        this.guard = new Perm();
    }

    public applyRoutes(app: Application): void {
        // ADMIN routes
        app.route('/resource/tracker')
            .get(this.guard.check('ADMIN'), this.controller.getAll)
            .post(this.guard.check('ADMIN'), this.controller.saveTracker);

        app.route('/resource/tracker/:trackerId')
            .get(this.guard.check('ADMIN'), this.controller.getSpecific);



        // USER + ADMIN routes
        app.route('/resource/me/tracker')
            .get(this.controller.getSelf);
    }
}