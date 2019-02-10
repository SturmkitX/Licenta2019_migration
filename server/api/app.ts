import * as Express from 'express';
import * as BodyParser from 'body-parser';

export class App {

    public app: Express.Application;

    constructor() {
        this.app = Express();
        this.config();
    }

    private config(): void{
        // support application/json type post data
        this.app.use(BodyParser.json());

        //support application/x-www-form-urlencoded post data
        this.app.use(BodyParser.urlencoded({ extended: false }));
    }
}

export default new App().app;