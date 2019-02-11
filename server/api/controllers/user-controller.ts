import { User } from '../models/user';
import { Request, Response } from 'express';
import * as bcrypt from 'bcryptjs';

export class UserController{

    constructor() {}

    public getAll(req: Request, res: Response): void {
        User.find({}, (err: any, users: Document[]) => {
            if (err) {
                res.status(500).send(err);
            } else {
                res.status(200).json(users);
            }
        });
    }

    public getUserMode(req: Request, res: Response): void {
        User.findById(req.params.userId, (err: any, userDoc: Document) => {
            const user: any = userDoc;
            if (err) {
                res.status(500).send(err);
            } else if (!user) {
                res.status(404).json(null);
            } else {
                user.password = null;
                res.status(200).json(user);
            }
        });
    }

    public saveUser(req: Request, res: Response): void {
        if (!req.body) {
            res.status(400).json(null);
        }

        const user = new User({
            firstName: req.body.firstName,
            lastName: req.body.lastName,
            email: req.body.email,
            password: bcrypt.hashSync(req.body.password, 10),
            trackers: req.body.trackers || []
        }).save((err, userSave) => {
            if (err) {
                res.status(500).send(err);
            } else {
                res.status(200).json(userSave);
            }
        });
    }
}
