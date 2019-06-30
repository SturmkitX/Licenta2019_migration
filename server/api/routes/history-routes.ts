import {Application} from "express";
import {HistoryController} from '../controllers/history-controller';
import * as Perm from 'express-jwt-permissions';

export class HistoryRoutes {

    private controller: HistoryController;
    private guard: any;

    constructor() {
        this.controller = new HistoryController();
        this.guard = new Perm();
    }

    public applyRoutes(app: Application): void {
        // ADMIN routes
        app.route('/resource/history')
            .get(this.guard.check('ADMIN'), this.controller.getAll)
            .post(this.guard.check([['ADMIN'], ['USER']]), this.controller.saveHistory.bind(this.controller));

        app.route('/resource/history/:historyId')
            .get(this.guard.check('ADMIN'), this.controller.getSpecific);



        // USER + ADMIN routes
        app.route('/resource/me/history/:trackerId')
            .get(this.guard.check([['ADMIN'], ['USER']]), this.controller.getSelfForTracker);

        // public routes
        app.route('/public/history')
            .post(this.controller.saveHistory.bind(this.controller));
    }
}